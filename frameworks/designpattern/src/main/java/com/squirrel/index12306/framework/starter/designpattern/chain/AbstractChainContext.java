package com.squirrel.index12306.framework.starter.designpattern.chain;

import com.squirrel.index12306.framework.starter.bases.ApplicationContextHolder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 抽象责任链上下文
 * 实现 commandRunner 接口，程序启动后会执行这个方法
 * @param <T>
 */
public final class AbstractChainContext<T> implements CommandLineRunner {

    private final Map<String, List<AbstractChainHandler>> abstractChainHandlerContainer = new HashMap<>();

    /**
     * 责任链组件执行
     * @param mark 责任链组件标识
     * @param requestParam 请求参数
     */
    public void handler(String mark,T requestParam) {
        List<AbstractChainHandler> abstractChainHandlers = abstractChainHandlerContainer.get(mark);
        if (CollectionUtils.isEmpty(abstractChainHandlers)) {
            throw new RuntimeException(String.format("[%s] Chain of Responsibility ID is undefined.", mark));
        }
        abstractChainHandlers.forEach(each -> each.handler(requestParam));
    }

    /**
     * 重写 run 方法
     * @param args 参数
     * @throws Exception 可能抛出异常
     */
    @Override
    public void run(String... args) throws Exception {
        // 获取所有 AbstractChainHandler 的实现类，以Map的形式返回，key是Bean的名字，value是实例
        Map<String, AbstractChainHandler> chainFilterMap =
                ApplicationContextHolder.getBeansOfType(AbstractChainHandler.class);
        // 遍历 Map 将相同 bean.mark() 的handler添加到 abstractChainHandlerContainer
        chainFilterMap.forEach((beanName,bean) -> {
            List<AbstractChainHandler> abstractChainHandlers = abstractChainHandlerContainer.get(bean.mark());
            if (CollectionUtils.isEmpty(abstractChainHandlers)) {
                abstractChainHandlers = new ArrayList<>();
            }
            abstractChainHandlers.add(bean);
            // 根据 handler 的 sorted 的接口排序就行了
            List<AbstractChainHandler> actualAbstractChainHandlers = abstractChainHandlers.stream()
                    .sorted(Comparator.comparing(Ordered::getOrder))
                    .collect(Collectors.toList());
            // 放入 container
            abstractChainHandlerContainer.put(bean.mark(),actualAbstractChainHandlers);
        });
    }
}
