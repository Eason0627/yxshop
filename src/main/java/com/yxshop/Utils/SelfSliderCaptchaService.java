package com.yxshop.Utils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 自研滑块验证码（无第三方依赖、无计费）
 *
 * 流程：
 *  1. GET /app/auth/slider/generate → 返回挑战（背景图+拼图块，targetX 不下发）
 *  2. 用户拖动滑块
 *  3. POST /app/auth/slider/verify {captchaId, x} → 验证位置，返回一次性 token
 *  4. 登录请求携带 token，后端调 verifyToken() 消费
 */
@Slf4j
@Service
public class SelfSliderCaptchaService {

    private static final String CHALLENGE_PREFIX = "slider:challenge:";
    private static final String TOKEN_PREFIX     = "slider:token:";

    static final int BG_WIDTH   = 310;
    static final int BG_HEIGHT  = 155;
    static final int PIECE_W    = 50;
    static final int PIECE_H    = 50;
    private static final int TOLERANCE     = 8;   // px 容差
    private static final int CHALLENGE_TTL = 300;  // 5 min
    private static final int TOKEN_TTL     = 180;  // 3 min

    private final StringRedisTemplate redis;
    private final Random rng = new Random();

    // 无 Redis 时的本地降级存储（仅用于开发环境）
    private final Map<String, String> localChallenges = new ConcurrentHashMap<>();
    private final Set<String>         localTokens     = Collections.synchronizedSet(new HashSet<>());

    public SelfSliderCaptchaService(ObjectProvider<StringRedisTemplate> redisProvider) {
        this.redis = redisProvider.getIfAvailable();
    }

    @Data
    public static class CaptchaChallenge {
        private String captchaId;
        private String bgBase64;
        private String pieceBase64;
        private int pieceY;
        private int bgWidth  = BG_WIDTH;
        private int bgHeight = BG_HEIGHT;
        private int pieceW   = PIECE_W;
        private int pieceH   = PIECE_H;
    }

    /** 生成新的滑块挑战 */
    public CaptchaChallenge generate() {
        int targetX = PIECE_W + rng.nextInt(BG_WIDTH - PIECE_W * 3); // 50~210
        int pieceY  = 20     + rng.nextInt(BG_HEIGHT - PIECE_H - 25); // 20~85

        String captchaId = UUID.randomUUID().toString().replace("-", "");
        String targetStr = String.valueOf(targetX);

        if (redis != null) {
            redis.opsForValue().set(CHALLENGE_PREFIX + captchaId, targetStr, CHALLENGE_TTL, TimeUnit.SECONDS);
        } else {
            localChallenges.put(captchaId, targetStr);
        }

        BufferedImage[] imgs = buildImages(targetX, pieceY);

        CaptchaChallenge c = new CaptchaChallenge();
        c.setCaptchaId(captchaId);
        c.setBgBase64("data:image/png;base64," + toBase64(imgs[0]));
        c.setPieceBase64("data:image/png;base64," + toBase64(imgs[1]));
        c.setPieceY(pieceY);
        return c;
    }

    /** 校验拖动位置，通过则返回一次性 token */
    public String verify(String captchaId, int userX) {
        if (captchaId == null || captchaId.isBlank()) {
            throw new IllegalArgumentException("验证码已过期，请刷新");
        }

        String stored;
        if (redis != null) {
            stored = redis.opsForValue().get(CHALLENGE_PREFIX + captchaId);
            if (stored != null) redis.delete(CHALLENGE_PREFIX + captchaId);
        } else {
            stored = localChallenges.remove(captchaId);
        }

        if (stored == null) throw new IllegalArgumentException("验证码已过期，请刷新");
        if (Math.abs(userX - Integer.parseInt(stored)) > TOLERANCE) {
            throw new IllegalArgumentException("验证失败，请重新拼图");
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        if (redis != null) {
            redis.opsForValue().set(TOKEN_PREFIX + token, "1", TOKEN_TTL, TimeUnit.SECONDS);
        } else {
            localTokens.add(token);
        }
        return token;
    }

    /** 消费一次性 token（登录时调用） */
    public boolean verifyToken(String token) {
        if (token == null || token.isBlank()) return false;
        if (redis != null) {
            Boolean exists = redis.hasKey(TOKEN_PREFIX + token);
            if (Boolean.TRUE.equals(exists)) {
                redis.delete(TOKEN_PREFIX + token);
                return true;
            }
            return false;
        }
        return localTokens.remove(token);
    }

    // ── 图片生成 ──────────────────────────────────────────────────────────────

    private BufferedImage[] buildImages(int targetX, int pieceY) {
        Color base = randomColor();
        BufferedImage baseBg = createBackground(base);
        BufferedImage piece  = extractPiece(baseBg, targetX, pieceY);
        drawHole(baseBg, targetX, pieceY);
        return new BufferedImage[]{baseBg, piece};
    }

    private Color randomColor() {
        Color[] palette = {
            new Color(255, 107,   0), // YXShop 橙
            new Color( 24, 144, 255), // 蓝
            new Color( 82, 196,  26), // 绿
            new Color(114,  46, 209), // 紫
            new Color( 19, 194, 194), // 青
        };
        return palette[rng.nextInt(palette.length)];
    }

    private BufferedImage createBackground(Color base) {
        Color dark  = base.darker().darker();
        Color light = base.brighter();

        BufferedImage img = new BufferedImage(BG_WIDTH, BG_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 渐变背景
        g.setPaint(new GradientPaint(0, 0, light, BG_WIDTH, BG_HEIGHT, dark));
        g.fillRect(0, 0, BG_WIDTH, BG_HEIGHT);

        // 网格线
        g.setColor(new Color(255, 255, 255, 22));
        for (int x = 0; x < BG_WIDTH;  x += 25) g.drawLine(x, 0, x, BG_HEIGHT);
        for (int y = 0; y < BG_HEIGHT; y += 25) g.drawLine(0, y, BG_WIDTH, y);

        // 装饰圆形
        g.setColor(new Color(255, 255, 255, 14));
        for (int i = 0; i < 4; i++) {
            int r = 18 + rng.nextInt(34);
            g.fillOval(rng.nextInt(BG_WIDTH) - r, rng.nextInt(BG_HEIGHT) - r, r * 2, r * 2);
        }

        // 水印文字
        g.setColor(new Color(255, 255, 255, 35));
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        FontMetrics fm = g.getFontMetrics();
        String txt = "YXShop";
        g.drawString(txt, (BG_WIDTH - fm.stringWidth(txt)) / 2, BG_HEIGHT / 2 + fm.getAscent() / 3);

        g.dispose();
        return img;
    }

    private BufferedImage extractPiece(BufferedImage src, int targetX, int pieceY) {
        BufferedImage piece = new BufferedImage(PIECE_W, BG_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = piece.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Shape clip = pieceShape(0, pieceY);
        g.setClip(clip);
        g.drawImage(src, -targetX, 0, null); // 偏移使 targetX 对齐 x=0
        g.setClip(null);

        // 高亮边框
        g.setColor(new Color(255, 255, 255, 200));
        g.setStroke(new BasicStroke(1.8f));
        g.draw(clip);

        // 阴影（偏移 1px）
        g.setColor(new Color(0, 0, 0, 40));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(pieceShape(1, pieceY + 1));

        g.dispose();
        return piece;
    }

    private void drawHole(BufferedImage bg, int targetX, int pieceY) {
        Graphics2D g = bg.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 深色填充
        g.setColor(new Color(0, 0, 0, 135));
        g.fill(pieceShape(targetX, pieceY));

        // 白色描边
        g.setColor(new Color(255, 255, 255, 70));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(pieceShape(targetX, pieceY));

        g.dispose();
    }

    /** 圆角矩形拼图形状 */
    private Shape pieceShape(int x, int y) {
        int r = 6;
        GeneralPath p = new GeneralPath();
        p.moveTo(x + r, y);
        p.lineTo(x + PIECE_W - r, y);
        p.quadTo(x + PIECE_W, y,            x + PIECE_W, y + r);
        p.lineTo(x + PIECE_W, y + PIECE_H - r);
        p.quadTo(x + PIECE_W, y + PIECE_H,  x + PIECE_W - r, y + PIECE_H);
        p.lineTo(x + r,       y + PIECE_H);
        p.quadTo(x,           y + PIECE_H,  x, y + PIECE_H - r);
        p.lineTo(x,           y + r);
        p.quadTo(x,           y,             x + r, y);
        p.closePath();
        return p;
    }

    private String toBase64(BufferedImage img) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("图片编码失败", e);
        }
    }
}
