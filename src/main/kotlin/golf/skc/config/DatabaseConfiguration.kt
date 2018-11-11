package golf.skc.config

import io.github.jhipster.config.JHipsterConstants
import io.github.jhipster.config.h2.H2ConfigurationHelper
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement
import java.sql.SQLException

@Configuration
@EnableJpaRepositories("golf.skc.repository")
@EnableJpaAuditing(auditorAwareRef = "springSecurityAuditorAware")
@EnableTransactionManagement
class DatabaseConfiguration(private val env: Environment) {
  private val log = LoggerFactory.getLogger(DatabaseConfiguration::class.java)

  private val validPortForH2: String
    @Throws(NumberFormatException::class)
    get() {
      var port = Integer.parseInt(env.getProperty("server.port")!!)
      port = if (port < 10000) {
        10000 + port
      } else {
        if (port < 63536) {
          port + 2000
        } else {
          port - 2000
        }
      }
      return port.toString()
    }

  /**
   * Open the TCP port for the H2 database, so it is available remotely.
   *
   * @return the H2 database TCP server
   * @throws SQLException if the server failed to start
   */
  @Suppress("ContextJavaBeanUnresolvedMethodsInspection")
  @Bean(initMethod = "start", destroyMethod = "stop")
  @Profile(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT)
  @Throws(SQLException::class)
  fun h2TCPServer(): Any {
    val port = validPortForH2
    log.debug("H2 database is available on port {}", port)
    return H2ConfigurationHelper.createServer(port)
  }
}
