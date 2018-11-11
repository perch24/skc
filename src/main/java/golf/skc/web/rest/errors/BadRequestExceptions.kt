package golf.skc.web.rest.errors

import org.zalando.problem.AbstractThrowableProblem
import org.zalando.problem.Exceptional
import org.zalando.problem.Status
import java.net.URI
import java.util.HashMap

open class BadRequestAlertException(type: URI?, defaultMessage: String?, val entityName: String, val errorKey: String)
    : AbstractThrowableProblem(type, defaultMessage, Status.BAD_REQUEST, entityName, null, null, getAlertParameters(entityName, errorKey)) {

    constructor(defaultMessage: String, entityName: String, errorKey: String) : this(ErrorConstants.DEFAULT_TYPE, defaultMessage, entityName, errorKey)

    override fun getCause(): Exceptional? {
        return super.cause
    }

    companion object {
        private fun getAlertParameters(entityName: String, errorKey: String): Map<String, Any> {
            val parameters = HashMap<String, Any>()
            parameters["message"] = "error.$errorKey"
            parameters["params"] = entityName
            return parameters
        }
    }
}

class LoginAlreadyUsedException :
    BadRequestAlertException(ErrorConstants.LOGIN_ALREADY_USED_TYPE, "Login name already used!", "userManagement", "userexists")

class EmailAlreadyUsedException :
    BadRequestAlertException(ErrorConstants.EMAIL_ALREADY_USED_TYPE, "Email is already in use!", "userManagement", "emailexists")
