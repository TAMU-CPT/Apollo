package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.apache.shiro.crypto.hash.Sha256Hash
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType
import org.restapidoc.pojo.RestApiVerb
import org.springframework.http.HttpStatus

@RestApi(name="RESTful User Services", description="Methods for managing users")
class RestfulUserController {

    @RestApiMethod(
        path = "/rest/v1/user/{userId}/organismPermissions",
        description = "Get organism permissions for user, returns an array of permission objects",
        verb = RestApiVerb.GET
    )
    @RestApiParams(params = [
            @RestApiParam(name = "apikey", type = "string", paramType = RestApiParamType.QUERY),
            @RestApiParam(name = "userId", type = "long", paramType = RestApiParamType.PATH, description = "User ID to fetch")
    ])
    def v1getOrganismPermissionsForUser() {
        // TODO: authenticate
        User requestingUser = User.findByApikey(params.apikey)
        // User we're learing about
        User queriedUser = User.findById(params.userId)
        // Data
        List<UserOrganismPermission> userOrganismPermissionList = UserOrganismPermission.findAllByUser(queriedUser)
        // Response
        render userOrganismPermissionList as JSON
    }
}
