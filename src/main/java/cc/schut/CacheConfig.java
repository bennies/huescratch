package cc.schut;

import com.google.common.cache.CacheBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.guava.GuavaCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {
    @Value("${cache.poll.milliseconds}")
    private long cachePollMilliSeconds;

    public final static String CACHE_CONF = "cacheConf";

    @Bean
    public Cache cache() {
        return new GuavaCache(CACHE_CONF, CacheBuilder.newBuilder()
                .expireAfterWrite(cachePollMilliSeconds, TimeUnit.MILLISECONDS)
                .build());
    }
}
