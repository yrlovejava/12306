package com.squirrel.index12306.framework.starter.distributedid.core.snowflake;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

/**
 * 使用随机数获取雪花 WorkId
 */
@Slf4j
public class RandomWorkIdChoose extends AbstractWorkIdChooseTemplate implements InitializingBean {

    @Override
    protected WorkIdWrapper chooseWorkId() {
        int start = 0,end = 31;
        return new WorkIdWrapper(getRandom(start, end), getRandom(start, end));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.chooseAndInit();
    }

    /**
     * 获取随机数
     * @param start 最小值
     * @param end 最大值
     * @return 随机数
     */
    private static long getRandom(int start, int end) {
        return (long) (Math.random() * (end - start + 1) + start);
    }
}
