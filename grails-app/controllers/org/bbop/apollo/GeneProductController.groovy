package org.bbop.apollo

import grails.converters.JSON
import grails.transaction.Transactional
import org.bbop.apollo.Feature
import org.bbop.apollo.GeneProductName
import org.bbop.apollo.User
import org.bbop.apollo.geneProduct.GeneProduct
import org.bbop.apollo.gwt.shared.GlobalPermissionEnum
import org.bbop.apollo.gwt.shared.PermissionEnum
import org.bbop.apollo.history.FeatureOperation
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType
import org.restapidoc.pojo.RestApiVerb

import static org.springframework.http.HttpStatus.NOT_FOUND
import static org.springframework.http.HttpStatus.UNAUTHORIZED

@RestApi(name = "Gene Product", description = "Methods for managing gene product annotaitons")
@Transactional(readOnly = true)
class GeneProductController {


    def permissionService
    def geneProductService
    def featureEventService
    def featureService

    @RestApiMethod(description = "Returns a JSON array of all suggested gene product names", path = "/geneProduct/search", verb = RestApiVerb.GET)
    @RestApiParams(params = [
//        @RestApiParam(name = "featureType", type = "string", paramType = RestApiParamType.QUERY, description = "Feature type")
        @RestApiParam(name = "organism", type = "string", paramType = RestApiParamType.QUERY, description = "Organism name")
        , @RestApiParam(name = "query", type = "string", paramType = RestApiParamType.QUERY, description = "Query value")
    ])
    def search() {
        try {
            JSONObject nameJson = permissionService.handleInput(request, params)
            log.debug "Showing gene product names ${nameJson}"
            Organism organism = nameJson.organism ? (Organism.findByCommonName(nameJson.organism) ?: Organism.findById(nameJson.organism)) : null

            List<GeneProductName> geneProductNameList = new ArrayList<>()
            List<String> geneProductNamesFiltered = new ArrayList<>()
            geneProductNameList.addAll(GeneProductName.executeQuery("select cc from GeneProductName cc where cc.name like :query", [query: nameJson.query + "%"]))
            // if there are organism filters for these canned comments for this organism, then apply them
            // TODO: somehow it is breaking this for organisms
            List<GeneProductNameOrganismFilter> geneProductNameOrganismFilters = GeneProductNameOrganismFilter.findAllByGeneProductNameInList(geneProductNameList)
            if (geneProductNameOrganismFilters) {
                GeneProductNameOrganismFilter.findAllByOrganismAndGeneProductNameInList(organism, geneProductNameList).each {
                    geneProductNamesFiltered.add(it.geneProductName.name)
                    geneProductNameList.remove(it.geneProductName)
                }
            }
            // otherwise ignore them
            else {
                geneProductNameList.each {
                    geneProductNamesFiltered.add(it.name)
                }
            }
            render geneProductNamesFiltered as JSON
        } catch (Exception e) {
            def error = [error: 'problem finding gene product names for : ' + e]
            log.error(error.error)
            render error as JSON
        }
    }

    @RestApiMethod(description = "Load gene product for feature", path = "/geneProduct", verb = RestApiVerb.POST)
    @RestApiParams(params = [
        @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
        , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
        , @RestApiParam(name = "uniqueName", type = "Feature uniqueName", paramType = RestApiParamType.QUERY, description = "Gene name to query on")
    ]
    )
    @Transactional
    def index() {
        JSONObject dataObject = permissionService.handleInput(request, params)
        if(!permissionService.checkLoginGlobalAndLocalPermissions(dataObject, GlobalPermissionEnum.USER,PermissionEnum.READ)){
            render status : UNAUTHORIZED
            return
        }
        Feature feature = Feature.findByUniqueName(dataObject.uniqueName as String)
        if (feature) {
            JSONObject annotations = geneProductService.getAnnotations(feature)
            // TODO: register with marshaller
            render annotations as JSON
        } else {
            render status: NOT_FOUND
        }
    }

//        {"gene":"e35ea570-f700-41fb-b479-70aa812174ad",
//        "goTerm":"GO:0060841",
//        "geneRelationship":"RO:0002616",
//        "evidenceCode":"ECO:0000335",
//        "negate":false,
//        "withOrFrom":["withprefix:12312321"],
//        "reference":"refprefix:44444444"
    @RestApiMethod(description = "Save New gene product for feature", path = "/geneProduct/save", verb = RestApiVerb.POST)
    @RestApiParams(params = [
        @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
        , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
        , @RestApiParam(name = "feature", type = "string", paramType = RestApiParamType.QUERY, description = "uniqueName of gene feature to query on")
        , @RestApiParam(name = "productName", type = "string", paramType = RestApiParamType.QUERY, description = "Name of gene product")
        , @RestApiParam(name = "alternate", type = "boolean", paramType = RestApiParamType.QUERY, description = "Alternate (default false)")
        , @RestApiParam(name = "evidenceCode", type = "string", paramType = RestApiParamType.QUERY, description = "Evidence (ECO) CURIE")
        , @RestApiParam(name = "evidenceCodeLAbel", type = "string", paramType = RestApiParamType.QUERY, description = "Evidence (ECO) Label")
        , @RestApiParam(name = "negate", type = "boolean", paramType = RestApiParamType.QUERY, description = "Negate evidence (default false)")
        , @RestApiParam(name = "withOrFrom", type = "string", paramType = RestApiParamType.QUERY, description = "JSON Array of with or from CURIE strings, e.g., {[\"UniProtKB:12312]]\"]}")
        , @RestApiParam(name = "reference", type = "string", paramType = RestApiParamType.QUERY, description = "Reference CURIE string, e.g., \"PMID:12312]]\"")
        , @RestApiParam(name = "notes", type = "string", paramType = RestApiParamType.QUERY, description = "JSON Array of reference CURIE strings, e.g., {[\"PMID:12312]]\"]}")
    ]
    )
    @Transactional
    def save() {
        JSONObject dataObject = permissionService.handleInput(request, params)
        if(!permissionService.checkLoginGlobalAndLocalPermissions(dataObject, GlobalPermissionEnum.USER,PermissionEnum.WRITE)){
            render status : UNAUTHORIZED
            return
        }
        User user = permissionService.getCurrentUser(dataObject)
        GeneProduct geneProduct = new GeneProduct()
        Feature feature = Feature.findByUniqueName(dataObject.feature)

        JSONObject originalFeatureJsonObject = featureService.convertFeatureToJSON(feature)

        geneProduct.feature = feature
        geneProduct.productName = dataObject.productName
        geneProduct.evidenceRef = dataObject.evidenceCode
        geneProduct.evidenceRefLabel = dataObject.evidenceCodeLabel
        geneProduct.alternate = dataObject.alternate ?: false
        geneProduct.withOrFromArray = dataObject.withOrFrom
        geneProduct.notesArray = dataObject.notes
        geneProduct.reference = dataObject.reference
        geneProduct.lastUpdated = new Date()
        geneProduct.dateCreated = new Date()
        geneProduct.addToOwners(user)
        feature.addToGeneProducts(geneProduct)
        geneProduct.save(flush: true, failOnError: true)
        GeneProductName.findOrSaveByName(geneProduct.productName)

        JSONArray oldFeaturesJsonArray = new JSONArray()
        oldFeaturesJsonArray.add(originalFeatureJsonObject)
        JSONArray newFeaturesJsonArray = new JSONArray()
        JSONObject currentFeatureJsonObject = featureService.convertFeatureToJSON(feature)
        newFeaturesJsonArray.add(currentFeatureJsonObject)

        featureEventService.addNewFeatureEvent(FeatureOperation.ADD_GENE_PRODUCT,
            feature.name,
            feature.uniqueName,
            dataObject,
            oldFeaturesJsonArray,
            newFeaturesJsonArray,
            user)

        JSONObject annotations = geneProductService.getAnnotations(feature)
        render annotations as JSON
    }

    @RestApiMethod(description = "Update existing gene products for feature", path = "/geneProduct/update", verb = RestApiVerb.POST)
    @RestApiParams(params = [
        @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
        , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
        , @RestApiParam(name = "id", type = "string", paramType = RestApiParamType.QUERY, description = "GO Annotation ID to update (required)")
        , @RestApiParam(name = "feature", type = "string", paramType = RestApiParamType.QUERY, description = "uniqueName of feature to query on")
        , @RestApiParam(name = "productName", type = "string", paramType = RestApiParamType.QUERY, description = "gene product name")
        , @RestApiParam(name = "alternate", type = "boolean", paramType = RestApiParamType.QUERY, description = "(default false) alternate")
        , @RestApiParam(name = "evidenceCode", type = "string", paramType = RestApiParamType.QUERY, description = "Evidence (ECO) CURIE")
        , @RestApiParam(name = "evidenceCodeLabel", type = "string", paramType = RestApiParamType.QUERY, description = "Evidence (ECO) Label")
        , @RestApiParam(name = "negate", type = "boolean", paramType = RestApiParamType.QUERY, description = "Negate evidence (default false)")
        , @RestApiParam(name = "withOrFrom", type = "string", paramType = RestApiParamType.QUERY, description = "JSON Array of with or from CURIE strings, e.g., {[\"UniProtKB:12312\"]}")
        , @RestApiParam(name = "reference", type = "string", paramType = RestApiParamType.QUERY, description = "Reference CURIE string, e.g., \"PMID:12312]]\"")
        , @RestApiParam(name = "notes", type = "string", paramType = RestApiParamType.QUERY, description = "JSON Array of notes strings, e.g., {[\"This is a note\"]}")
    ]
    )
    @Transactional
    def update() {
        JSONObject dataObject = permissionService.handleInput(request, params)
        if(!permissionService.checkLoginGlobalAndLocalPermissions(dataObject, GlobalPermissionEnum.USER,PermissionEnum.WRITE)){
            render status : UNAUTHORIZED
            return
        }
        User user = permissionService.getCurrentUser(dataObject)
        Feature feature = Feature.findByUniqueName(dataObject.feature)

        JSONObject originalFeatureJsonObject = featureService.convertFeatureToJSON(feature)


        GeneProduct geneProduct = GeneProduct.findById(dataObject.id)
        geneProduct.feature = feature
        geneProduct.productName = dataObject.productName
        geneProduct.evidenceRef = dataObject.evidenceCode
        geneProduct.evidenceRefLabel = dataObject.evidenceCodeLabel
        geneProduct.alternate = dataObject.alternate ?: false
        geneProduct.withOrFromArray = dataObject.withOrFrom
        geneProduct.notesArray = dataObject.notes
        geneProduct.reference = dataObject.reference
        geneProduct.lastUpdated = new Date()
        geneProduct.dateCreated = new Date()
        geneProduct.addToOwners(user)
        GeneProductName.findOrSaveByName(geneProduct.productName)
        geneProduct.save(flush: true, failOnError: true, insert: false)

        JSONArray oldFeaturesJsonArray = new JSONArray()
        oldFeaturesJsonArray.add(originalFeatureJsonObject)
        JSONArray newFeaturesJsonArray = new JSONArray()
        JSONObject currentFeatureJsonObject = featureService.convertFeatureToJSON(feature)
        newFeaturesJsonArray.add(currentFeatureJsonObject)

        featureEventService.addNewFeatureEvent(FeatureOperation.UPDATE_GENE_PRODUCT,
            feature.name,
            feature.uniqueName,
            dataObject,
            oldFeaturesJsonArray,
            newFeaturesJsonArray,
            user)

        JSONObject annotations = geneProductService.getAnnotations(feature)
        render annotations as JSON
    }

    @RestApiMethod(description = "Delete existing gene product for feature", path = "/geneProduct/delete", verb = RestApiVerb.POST)
    @RestApiParams(params = [
        @RestApiParam(name = "username", type = "email", paramType = RestApiParamType.QUERY)
        , @RestApiParam(name = "password", type = "password", paramType = RestApiParamType.QUERY)
        , @RestApiParam(name = "id", type = "string", paramType = RestApiParamType.QUERY, description = "GO Annotation ID to delete (required)")
        , @RestApiParam(name = "uniqueName", type = "string", paramType = RestApiParamType.QUERY, description = "Feature uniqueName to remove feature from")
    ]
    )
    @Transactional
    def delete() {
        JSONObject dataObject = permissionService.handleInput(request, params)
        if(!permissionService.checkLoginGlobalAndLocalPermissions(dataObject, GlobalPermissionEnum.USER,PermissionEnum.WRITE)){
            render status : UNAUTHORIZED
            return
        }
        User user = permissionService.getCurrentUser(dataObject)

        Feature feature = Feature.findByUniqueName(dataObject.feature)
        JSONObject originalFeatureJsonObject = featureService.convertFeatureToJSON(feature)

        GeneProduct geneProduct = GeneProduct.findById(dataObject.id)
        feature.removeFromGeneProducts(geneProduct)
        geneProduct.delete(flush: true)

        JSONArray oldFeaturesJsonArray = new JSONArray()
        oldFeaturesJsonArray.add(originalFeatureJsonObject)
        JSONArray newFeaturesJsonArray = new JSONArray()
        JSONObject currentFeatureJsonObject = featureService.convertFeatureToJSON(feature)
        newFeaturesJsonArray.add(currentFeatureJsonObject)

        featureEventService.addNewFeatureEvent(FeatureOperation.REMOVE_GENE_PRODUCT,
            feature.name,
            feature.uniqueName,
            dataObject,
            oldFeaturesJsonArray,
            newFeaturesJsonArray,
            user)

        JSONObject annotations = geneProductService.getAnnotations(feature)
        render annotations as JSON
    }

}
