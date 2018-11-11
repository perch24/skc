package golf.skc.service

import golf.skc.config.Constants

import golf.skc.SkcApp
import golf.skc.domain.User
import io.github.jhipster.config.JHipsterProperties
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.MockitoAnnotations
import org.mockito.Spy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.MessageSource
import org.springframework.mail.MailSendException
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.test.context.junit4.SpringRunner
import org.thymeleaf.spring5.SpringTemplateEngine

import javax.mail.Multipart
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import java.io.ByteArrayOutputStream

import org.assertj.core.api.Assertions.assertThat
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*

@RunWith(SpringRunner::class)
@SpringBootTest(classes = arrayOf(SkcApp::class))
class MailServiceIntTest {

    @Autowired
    private val jHipsterProperties: JHipsterProperties? = null

    @Autowired
    private val messageSource: MessageSource? = null

    @Autowired
    private val templateEngine: SpringTemplateEngine? = null

    @Spy
    private val javaMailSender: JavaMailSenderImpl? = null

    @Captor
    private val messageCaptor: ArgumentCaptor<MimeMessage>? = null

    private var mailService: MailService? = null

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        doNothing().`when`<JavaMailSenderImpl>(javaMailSender).send(any(MimeMessage::class.java))
        mailService = MailService(jHipsterProperties!!, javaMailSender!!, messageSource!!, templateEngine!!)
    }

    @Test
    @Throws(Exception::class)
    fun testSendEmail() {
        mailService!!.sendEmail("john.doe@example.com", "testSubject", "testContent", false, false)
        verify<JavaMailSenderImpl>(javaMailSender).send(messageCaptor!!.capture())
        val message = messageCaptor.value
        assertThat(message.subject).isEqualTo("testSubject")
        assertThat(message.allRecipients[0].toString()).isEqualTo("john.doe@example.com")
        assertThat(message.from[0].toString()).isEqualTo("test@localhost")
        assertThat(message.content).isInstanceOf(String::class.java)
        assertThat(message.content.toString()).isEqualTo("testContent")
        assertThat(message.dataHandler.contentType).isEqualTo("text/plain; charset=UTF-8")
    }

    @Test
    @Throws(Exception::class)
    fun testSendHtmlEmail() {
        mailService!!.sendEmail("john.doe@example.com", "testSubject", "testContent", false, true)
        verify<JavaMailSenderImpl>(javaMailSender).send(messageCaptor!!.capture())
        val message = messageCaptor.value
        assertThat(message.subject).isEqualTo("testSubject")
        assertThat(message.allRecipients[0].toString()).isEqualTo("john.doe@example.com")
        assertThat(message.from[0].toString()).isEqualTo("test@localhost")
        assertThat(message.content).isInstanceOf(String::class.java)
        assertThat(message.content.toString()).isEqualTo("testContent")
        assertThat(message.dataHandler.contentType).isEqualTo("text/html;charset=UTF-8")
    }

    @Test
    @Throws(Exception::class)
    fun testSendMultipartEmail() {
        mailService!!.sendEmail("john.doe@example.com", "testSubject", "testContent", true, false)
        verify<JavaMailSenderImpl>(javaMailSender).send(messageCaptor!!.capture())
        val message = messageCaptor.value
        val mp = message.content as MimeMultipart
        val part = (mp.getBodyPart(0).content as MimeMultipart).getBodyPart(0) as MimeBodyPart
        val aos = ByteArrayOutputStream()
        part.writeTo(aos)
        assertThat(message.subject).isEqualTo("testSubject")
        assertThat(message.allRecipients[0].toString()).isEqualTo("john.doe@example.com")
        assertThat(message.from[0].toString()).isEqualTo("test@localhost")
        assertThat(message.content).isInstanceOf(Multipart::class.java)
        assertThat(aos.toString()).isEqualTo("\r\ntestContent")
        assertThat(part.dataHandler.contentType).isEqualTo("text/plain; charset=UTF-8")
    }

    @Test
    @Throws(Exception::class)
    fun testSendMultipartHtmlEmail() {
        mailService!!.sendEmail("john.doe@example.com", "testSubject", "testContent", true, true)
        verify<JavaMailSenderImpl>(javaMailSender).send(messageCaptor!!.capture())
        val message = messageCaptor.value
        val mp = message.content as MimeMultipart
        val part = (mp.getBodyPart(0).content as MimeMultipart).getBodyPart(0) as MimeBodyPart
        val aos = ByteArrayOutputStream()
        part.writeTo(aos)
        assertThat(message.subject).isEqualTo("testSubject")
        assertThat(message.allRecipients[0].toString()).isEqualTo("john.doe@example.com")
        assertThat(message.from[0].toString()).isEqualTo("test@localhost")
        assertThat(message.content).isInstanceOf(Multipart::class.java)
        assertThat(aos.toString()).isEqualTo("\r\ntestContent")
        assertThat(part.dataHandler.contentType).isEqualTo("text/html;charset=UTF-8")
    }

    @Test
    @Throws(Exception::class)
    fun testSendEmailFromTemplate() {
        val user = User()
        user.login = "john"
        user.email = "john.doe@example.com"
        user.langKey = "en"
        mailService!!.sendEmailFromTemplate(user, "mail/testEmail", "email.test.title")
        verify<JavaMailSenderImpl>(javaMailSender).send(messageCaptor!!.capture())
        val message = messageCaptor.value
        assertThat(message.subject).isEqualTo("test title")
        assertThat(message.allRecipients[0].toString()).isEqualTo(user.email)
        assertThat(message.from[0].toString()).isEqualTo("test@localhost")
        assertThat(message.content.toString()).isEqualToNormalizingNewlines("<html>test title, http://127.0.0.1:8080, john</html>\n")
        assertThat(message.dataHandler.contentType).isEqualTo("text/html;charset=UTF-8")
    }

    @Test
    @Throws(Exception::class)
    fun testSendActivationEmail() {
        val user = User()
        user.langKey = Constants.DEFAULT_LANGUAGE
        user.login = "john"
        user.email = "john.doe@example.com"
        mailService!!.sendActivationEmail(user)
        verify<JavaMailSenderImpl>(javaMailSender).send(messageCaptor!!.capture())
        val message = messageCaptor.value
        assertThat(message.allRecipients[0].toString()).isEqualTo(user.email)
        assertThat(message.from[0].toString()).isEqualTo("test@localhost")
        assertThat(message.content.toString()).isNotEmpty()
        assertThat(message.dataHandler.contentType).isEqualTo("text/html;charset=UTF-8")
    }

    @Test
    @Throws(Exception::class)
    fun testCreationEmail() {
        val user = User()
        user.langKey = Constants.DEFAULT_LANGUAGE
        user.login = "john"
        user.email = "john.doe@example.com"
        mailService!!.sendCreationEmail(user)
        verify<JavaMailSenderImpl>(javaMailSender).send(messageCaptor!!.capture())
        val message = messageCaptor.value
        assertThat(message.allRecipients[0].toString()).isEqualTo(user.email)
        assertThat(message.from[0].toString()).isEqualTo("test@localhost")
        assertThat(message.content.toString()).isNotEmpty()
        assertThat(message.dataHandler.contentType).isEqualTo("text/html;charset=UTF-8")
    }

    @Test
    @Throws(Exception::class)
    fun testSendPasswordResetMail() {
        val user = User()
        user.langKey = Constants.DEFAULT_LANGUAGE
        user.login = "john"
        user.email = "john.doe@example.com"
        mailService!!.sendPasswordResetMail(user)
        verify<JavaMailSenderImpl>(javaMailSender).send(messageCaptor!!.capture())
        val message = messageCaptor.value
        assertThat(message.allRecipients[0].toString()).isEqualTo(user.email)
        assertThat(message.from[0].toString()).isEqualTo("test@localhost")
        assertThat(message.content.toString()).isNotEmpty()
        assertThat(message.dataHandler.contentType).isEqualTo("text/html;charset=UTF-8")
    }

    @Test
    @Throws(Exception::class)
    fun testSendEmailWithException() {
        doThrow(MailSendException::class.java).`when`<JavaMailSenderImpl>(javaMailSender).send(any(MimeMessage::class.java))
        mailService!!.sendEmail("john.doe@example.com", "testSubject", "testContent", false, false)
    }

}
