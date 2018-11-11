package golf.skc.config

import io.github.jhipster.config.JHipsterConstants

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cloud.config.java.AbstractCloudConfig
import org.springframework.context.annotation.*

import javax.sql.DataSource
import org.springframework.boot.context.properties.ConfigurationProperties


@Configuration
@Profile(JHipsterConstants.SPRING_PROFILE_CLOUD)
class CloudDatabaseConfiguration : AbstractCloudConfig() {
    private val log = LoggerFactory.getLogger(CloudDatabaseConfiguration::class.java)

    @Bean
    @ConfigurationProperties(CLOUD_CONFIGURATION_HIKARI_PREFIX)
    fun dataSource(): DataSource {
        log.info("Configuring JDBC datasource from a cloud provider")
        return connectionFactory().dataSource()
    }

    companion object {
        private const val CLOUD_CONFIGURATION_HIKARI_PREFIX = "spring.datasource.hikari"
    }
}
