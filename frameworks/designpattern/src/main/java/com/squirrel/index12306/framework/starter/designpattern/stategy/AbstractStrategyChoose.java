package com.squirrel.index12306.framework.starter.designpattern.stategy;

import com.squirrel.index12306.framework.starter.bases.ApplicationContextHolder;
import com.squirrel.index12306.framework.starter.bases.init.ApplicationInitializingEvent;
import com.squirrel.index12306.framework.starter.convention.exception.ServiceException;
import org.springframework.context.ApplicationListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 策略选择器
 */
public class AbstractStrategyChoose implements ApplicationListener<ApplicationInitializingEvent> {

    /**
     * 执行策略集合
     */
    private final Map<String, AbstractExecuteStrategy> abstractExecuteStrategyMap = new HashMap<>();

    /**
     * 根据 mark 查询具体策略并
     *
     * @param mark 策略标识
     * @return 实际执行策略
     */
    public AbstractExecuteStrategy choose(String mark) {
        return Optional.ofNullable(abstractExecuteStrategyMap.get(mark)).orElseThrow(() -> new ServiceException(String.format("[%s] 策略未定义", mark)));
    }

    /**
     * 根据 mark 查询具体策略并执行
     *
     * @param mark         策略标识
     * @param requestParam 执行策略入参
     * @param <REQUEST>    执行策略入参泛型
     */
    public <REQUEST> void chooseAndExecute(String mark, REQUEST requestParam) {
        AbstractExecuteStrategy executeStrategy = this.choose(mark);
        executeStrategy.execute(requestParam);
    }

    /**
     * 根据 mark 查询具体策略并执行，带返回结果
     * @param mark 策略标识
     * @param requestParam 执行策略入参
     * @return 执行结果
     * @param <REQUEST> 执行策略入参泛型
     * @param <RESPONSE> 执行结果出参泛型
     */
    public <REQUEST,RESPONSE> RESPONSE chooseAndExecuteResp(String mark, REQUEST requestParam) {
        AbstractExecuteStrategy executeStrategy = this.choose(mark);
        return (RESPONSE) executeStrategy.executeResp(requestParam);
    }

    @Override
    public void onApplicationEvent(ApplicationInitializingEvent event) {
        Map<String, AbstractExecuteStrategy> actual = ApplicationContextHolder.getBeansOfType(AbstractExecuteStrategy.class);
        actual.forEach((beanName, bean) -> {
            AbstractExecuteStrategy beanExist = abstractExecuteStrategyMap.get(bean.mark());
            if (beanExist != null) {
                throw new ServiceException(String.format("[%s] Duplicate execution policy", bean.mark()));
            }
            abstractExecuteStrategyMap.put(bean.mark(), bean);
        });
    }
}