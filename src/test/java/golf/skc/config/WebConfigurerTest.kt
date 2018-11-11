package golf.skc.config

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.servlet.InstrumentedFilter
import com.codahale.metrics.servlets.MetricsServlet
import io.github.jhipster.config.JHipsterConstants
import io.github.jhipster.config.JHipsterProperties
import io.github.jhipster.web.filter.CachingHttpHeadersFilter
import io.undertow.Undertow
import io.undertow.UndertowOptions
import org.apache.commons.io.FilenameUtils

import org.h2.server.web.WebServlet
import org.junit.Before
import org.junit.Test
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory
import org.springframework.http.HttpHeaders
import org.springframework.mock.env.MockEnvironment
import org.springframework.mock.web.MockServletContext
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.xnio.OptionMap

import javax.servlet.*
import java.util.*

import org.assertj.core.api.Assertions.assertThat
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.*
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder

/**
 * Unit tests for the WebConfigurer class.
 *
 * @see WebConfigurer
 */
class WebConfigurerTest {

    private var webConfigurer: WebConfigurer? = null

    private var servletContext: MockServletContext? = null

    private var env: MockEnvironment? = null

    private var props: JHipsterProperties? = null

    private var metricRegistry: MetricRegistry? = null

    @Before
    fun setup() {
        servletContext = spy(MockServletContext())
        doReturn(mock(FilterRegistration.Dynamic::class.java))
            .`when`<MockServletContext>(servletContext).addFilter(anyString(), any(Filter::class.java))
        doReturn(mock(ServletRegistration.Dynamic::class.java))
            .`when`<MockServletContext>(servletContext).addServlet(anyString(), any(Servlet::class.java))

        env = MockEnvironment()
        props = JHipsterProperties()

        webConfigurer = WebConfigurer(env!!, props!!)
        metricRegistry = MetricRegistry()
        webConfigurer!!.setMetricRegistry(metricRegistry!!)
    }

    @Test
    @Throws(ServletException::class)
    fun testStartUpProdServletContext() {
        env!!.setActiveProfiles(JHipsterConstants.SPRING_PROFILE_PRODUCTION)
        webConfigurer!!.onStartup(servletContext!!)

        assertThat(servletContext!!.getAttribute(InstrumentedFilter.REGISTRY_ATTRIBUTE)).isEqualTo(metricRegistry)
        assertThat(servletContext!!.getAttribute(MetricsServlet.METRICS_REGISTRY)).isEqualTo(metricRegistry)
        verify<MockServletContext>(servletContext).addFilter(ArgumentMatchers.eq("webappMetricsFilter"), any(InstrumentedFilter::class.java))
        verify<MockServletContext>(servletContext).addServlet(ArgumentMatchers.eq("metricsServlet"), any(MetricsServlet::class.java))
        verify<MockServletContext>(servletContext).addFilter(ArgumentMatchers.eq("cachingHttpHeadersFilter"), any(CachingHttpHeadersFilter::class.java))
        verify<MockServletContext>(servletContext, never()).addServlet(ArgumentMatchers.eq("H2Console"), any(WebServlet::class.java))
    }

    @Test
    @Throws(ServletException::class)
    fun testStartUpDevServletContext() {
        env!!.setActiveProfiles(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT)
        webConfigurer!!.onStartup(servletContext!!)

        assertThat(servletContext!!.getAttribute(InstrumentedFilter.REGISTRY_ATTRIBUTE)).isEqualTo(metricRegistry)
        assertThat(servletContext!!.getAttribute(MetricsServlet.METRICS_REGISTRY)).isEqualTo(metricRegistry)
        verify<MockServletContext>(servletContext).addFilter(ArgumentMatchers.eq("webappMetricsFilter"), any(InstrumentedFilter::class.java))
        verify<MockServletContext>(servletContext).addServlet(ArgumentMatchers.eq("metricsServlet"), any(MetricsServlet::class.java))
        verify<MockServletContext>(servletContext, never()).addFilter(ArgumentMatchers.eq("cachingHttpHeadersFilter"), any(CachingHttpHeadersFilter::class.java))
        verify<MockServletContext>(servletContext).addServlet(ArgumentMatchers.eq("H2Console"), any(WebServlet::class.java))
    }

    @Test
    fun testCustomizeServletContainer() {
        env!!.setActiveProfiles(JHipsterConstants.SPRING_PROFILE_PRODUCTION)
        val container = UndertowServletWebServerFactory()
        webConfigurer!!.customize(container)
        assertThat(container.mimeMappings.get("abs")).isEqualTo("audio/x-mpeg")
        assertThat(container.mimeMappings.get("html")).isEqualTo("text/html;charset=utf-8")
        assertThat(container.mimeMappings.get("json")).isEqualTo("text/html;charset=utf-8")
        if (container.documentRoot != null) {
            assertThat(container.documentRoot.path).isEqualTo(FilenameUtils.separatorsToSystem("build/www"))
        }

        val builder = Undertow.builder()
        container.builderCustomizers.forEach { c -> c.customize(builder) }
        val serverOptions = ReflectionTestUtils.getField(builder, "serverOptions") as OptionMap.Builder
        assertThat(serverOptions.map.get(UndertowOptions.ENABLE_HTTP2) == null).isTrue()
    }

    @Test
    fun testUndertowHttp2Enabled() {
        props!!.http.setVersion(JHipsterProperties.Http.Version.V_2_0)
        val container = UndertowServletWebServerFactory()
        webConfigurer!!.customize(container)
        val builder = Undertow.builder()
        container.builderCustomizers.forEach { c -> c.customize(builder) }
        val serverOptions = ReflectionTestUtils.getField(builder, "serverOptions") as OptionMap.Builder
        assertThat(serverOptions.map.get(UndertowOptions.ENABLE_HTTP2)).isTrue()
    }

    @Test
    @Throws(Exception::class)
    fun testCorsFilterOnApiPath() {
        props!!.cors.allowedOrigins = listOf("*")
        props!!.cors.allowedMethods = Arrays.asList("GET", "POST", "PUT", "DELETE")
        props!!.cors.allowedHeaders = listOf("*")
        props!!.cors.maxAge = 1800L
        props!!.cors.allowCredentials = true

        val mockMvc = MockMvcBuilders.standaloneSetup(WebConfigurerTestController())
            .addFilters<StandaloneMockMvcBuilder>(webConfigurer!!.corsFilter())
            .build()

        mockMvc.perform(
            options("/api/test-cors")
                .header(HttpHeaders.ORIGIN, "other.domain.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
            .andExpect(status().isOk)
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "other.domain.com"))
            .andExpect(header().string(HttpHeaders.VARY, "Origin"))
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT,DELETE"))
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "1800"))

        mockMvc.perform(
            get("/api/test-cors")
                .header(HttpHeaders.ORIGIN, "other.domain.com"))
            .andExpect(status().isOk)
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "other.domain.com"))
    }

    @Test
    @Throws(Exception::class)
    fun testCorsFilterOnOtherPath() {
        props!!.cors.allowedOrigins = listOf("*")
        props!!.cors.allowedMethods = Arrays.asList("GET", "POST", "PUT", "DELETE")
        props!!.cors.allowedHeaders = listOf("*")
        props!!.cors.maxAge = 1800L
        props!!.cors.allowCredentials = true

        val mockMvc = MockMvcBuilders.standaloneSetup(WebConfigurerTestController())
            .addFilters<StandaloneMockMvcBuilder>(webConfigurer!!.corsFilter())
            .build()

        mockMvc.perform(
            get("/test/test-cors")
                .header(HttpHeaders.ORIGIN, "other.domain.com"))
            .andExpect(status().isOk)
            .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
    }

    @Test
    @Throws(Exception::class)
    fun testCorsFilterDeactivated() {
        props!!.cors.allowedOrigins = null

        val mockMvc = MockMvcBuilders.standaloneSetup(WebConfigurerTestController())
            .addFilters<StandaloneMockMvcBuilder>(webConfigurer!!.corsFilter())
            .build()

        mockMvc.perform(
            get("/api/test-cors")
                .header(HttpHeaders.ORIGIN, "other.domain.com"))
            .andExpect(status().isOk)
            .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
    }

    @Test
    @Throws(Exception::class)
    fun testCorsFilterDeactivated2() {
        props!!.cors.allowedOrigins = ArrayList()

        val mockMvc = MockMvcBuilders.standaloneSetup(WebConfigurerTestController())
            .addFilters<StandaloneMockMvcBuilder>(webConfigurer!!.corsFilter())
            .build()

        mockMvc.perform(
            get("/api/test-cors")
                .header(HttpHeaders.ORIGIN, "other.domain.com"))
            .andExpect(status().isOk)
            .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
    }
}
