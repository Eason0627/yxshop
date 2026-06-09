package com.yxshop.Module.Cart.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yxshop.Module.Cart.Dto.CartItemDto;
import com.yxshop.Module.Cart.Dto.CartSelectionDto;
import com.yxshop.Module.Cart.Entity.CartItemEntity;
import com.yxshop.Module.Cart.Vo.CartSummaryVo;

import java.util.List;

public interface CartItemService extends IService<CartItemEntity> {
    CartSummaryVo listCart(Long userId);

    CartSummaryVo addItem(Long userId, CartItemDto dto);

    CartSummaryVo updateItem(Long userId, Long itemId, CartItemDto dto);

    CartSummaryVo updateSelection(Long userId, CartSelectionDto dto);

    CartSummaryVo deleteItems(Long userId, List<Long> itemIds);

    CartSummaryVo clearInvalid(Long userId);

    CartSummaryVo selectedSettlement(Long userId);
}
