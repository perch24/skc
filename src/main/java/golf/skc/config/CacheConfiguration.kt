package golf.skc.config

import golf.skc.repository.UserRepository
import io.github.jhipster.config.JHipsterProperties
import io.github.jhipster.config.jcache.BeanClassLoaderAwareJCacheRegionFactory
import org.ehcache.config.builders.CacheConfigurationBuilder
import org.ehcache.config.builders.ExpiryPolicyBuilder
import org.ehcache.config.builders.ResourcePoolsBuilder
import org.ehcache.jsr107.Eh107Configuration
import org.springframework.boot.autoconfigure.cache.JCacheManagerCustomizer
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@EnableCaching
class CacheConfiguration(jHipsterProperties: JHipsterProperties) {
  private val jcacheConfiguration: javax.cache.configuration.Configuration<Any, Any>

  init {
    BeanClassLoaderAwareJCacheRegionFactory.setBeanClassLoader(this.javaClass.classLoader)
    val ehcache = jHipsterProperties.cache.ehcache

    jcacheConfiguration = Eh107Configuration.fromEhcacheCacheConfiguration(
      CacheConfigurationBuilder.newCacheConfigurationBuilder(Any::class.java, Any::class.java,
        ResourcePoolsBuilder.heap(ehcache.maxEntries))
        .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(ehcache.timeToLiveSeconds.toLong())))
        .build())
  }

  @Bean
  fun cacheManagerCustomizer(): JCacheManagerCustomizer {
    return JCacheManagerCustomizer {
      it.createCache(UserRepository.USERS_BY_LOGIN_CACHE, jcacheConfiguration)
      it.createCache(UserRepository.USERS_BY_EMAIL_CACHE, jcacheConfiguration)
      it.createCache(golf.skc.domain.User::class.java.name, jcacheConfiguration)
      it.createCache(golf.skc.domain.Authority::class.java.name, jcacheConfiguration)
      it.createCache(golf.skc.domain.User::class.java.name + ".authorities", jcacheConfiguration)
    }
  }
}
