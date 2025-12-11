package com.redis_cache;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
public class RedisListenerConfig {

    @Autowired
    private SseService sseService;
    @Autowired
    private SearchService searchService;

    @Autowired
    private RedisConnectionFactory connectionFactory;

    @Bean
    public RedisMessageListenerContainer redisContainer() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // SortedSet 변경 시 발생하는 이벤트 구독
        container.addMessageListener(eventListener(),
                new PatternTopic("__keyevent@0__:*"));

        return container;
    }

    @Bean
    public MessageListener eventListener() {
        return new MessageListener() {
            @Override
            public void onMessage(Message message, byte[] patternBytes) {
                String key = new String(message.getBody());     // 변경된 key 이름

                if ("popular_keywords".equals(key)) {
                    sseService.broadcast("updated");  // 인기검색어 업데이트 알림
                    searchService.evictPopularCache();
                }
            }
        };
    }
}
