package com.yxshop;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class YXshopApplicationTests {

    @Test
    void contextLoads() {
        PageHelper.startPage(1,10,true);
    }

}
