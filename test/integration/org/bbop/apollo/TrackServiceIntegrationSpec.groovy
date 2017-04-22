package org.bbop.apollo

import grails.converters.JSON
import org.bbop.apollo.gwt.shared.FeatureStringEnum
import org.bbop.apollo.gwt.shared.projection.ProjectionChunkList
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.bbop.apollo.gwt.shared.projection.MultiSequenceProjection
import spock.lang.Ignore
import spock.lang.IgnoreRest

//import spock.lang.IgnoreRest

class TrackServiceIntegrationSpec extends AbstractIntegrationSpec {

    def trackService

    def setup() {

        Organism organism = Organism.first()
        organism.directory = "test/integration/resources/sequences/honeybee-tracks/"
        organism.save(failOnError: true, flush: true)

        new Sequence(
                length: 75085
                , seqChunkSize: 20000
                , start: 0
                , end: 75085
                , organism: organism
                , name: "Group11.4"
        ).save(failOnError: true)

        new Sequence(
                length: 1566327
                , seqChunkSize: 20000
                , start: 0
                , end: 1566327
                , organism: organism
                , name: "Group11.6"
        ).save(failOnError: true)

        new Sequence(
                length: 78258
                , seqChunkSize: 20000
                , start: 0
                , end: 78258
                , organism: organism
                , name: "GroupUn87"
        ).save(failOnError: true)

        new Sequence(
                length: 494196
                , seqChunkSize: 20000
                , start: 0
                , end: 494196
                , organism: organism
                , name: "Group4.1"
        ).save(failOnError: true)
    }

    /**
     *  GroupUn87: Projected: 0,213 <-> 718,838   (4 groups), Unprojected: 9966,10179 (first)  45455,45575 (last)
     *
     *  Group11.4: Projected: 0,2538 <-> 14574,15734  (5 groups), Unprojected: 10257,18596 (first) 62507,64197 (last)
     */
    void "non-projection of contiguous tracks should work"() {

        given: "proper inputs"
        JSONArray sequenceStrings = new JSONArray('[{name:"GroupUn87"}, {name:"Group11.4"}]')
        String dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/{\"padding\":0, \"projection\":\"None\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"GroupUn87\"},{\"name\":\"Group11.4\"}], \"label\":\"GroupUn87::Group11.4\"}:-1..-1/trackData.json"
        String refererLoc = "{\"padding\":0, \"projection\":\"None\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"GroupUn87\"},{\"name\":\"Group11.4\"}], \"label\":\"GroupUn87::Group11.4\"}:-1..-1:1..16607"

        when: "we get the projected track data "
        JSONObject trackObject = trackService.projectTrackData(sequenceStrings, dataFileName, refererLoc, Organism.first())
        Sequence un87Sequence = Sequence.findByName("GroupUn87")
        println trackObject as JSON

        then: "we expect to get sane results"
        assert trackObject.featureCount == 10
        def minStart = trackObject.minStart
        def maxEnd = trackObject.maxEnd
        JSONArray nclist = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)
        assert nclist.size() == 9
        JSONArray firstArray = nclist.getJSONArray(0)
        assert firstArray.getInt(1) == 9966
        assert firstArray.getInt(2) == 10179
        assert firstArray.getInt(3) == 1

        JSONArray lastFirstArray = nclist.getJSONArray(3)
//        assert firstArray.getInt(2)==0
        assert lastFirstArray.getInt(1) == 45455// end of the first set
        assert lastFirstArray.getInt(2) == 45575// end of the first set
        assert lastFirstArray.getInt(3) == 1 // end of the first set

        // the next array should go somewhere completely else
        JSONArray firstLastArray = nclist.getJSONArray(4)
        assert firstLastArray.getInt(1) == 10257 + un87Sequence.length // start of the last set
        assert firstLastArray.getInt(2) == 18596 + un87Sequence.length // start of the last set
        assert firstLastArray.getInt(3) == 1

        JSONArray lastLastArray = nclist.getJSONArray(8)
        assert lastLastArray.getInt(1) == 62507 + un87Sequence.length // end of the last set
        assert lastLastArray.getInt(2) == 64197 + un87Sequence.length // end of the last set
        assert lastLastArray.getInt(3) == -1
    }

    /**
     *  Group11.4: Projected: 0,2538 <-> 14574,15734  (5 groups), Unprojected: 10257,18596 (first) 62507,64197 (last)
     *
     */
    void "un-projected 11.4 individually"() {
        given: "proper inputs"
        JSONArray sequenceStrings = new JSONArray('[{name:"Group11.4"}]')
        String dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/{\"padding\":0, \"projection\":\"None\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"Group11.4\"}], \"label\":\"Group11.4\"}:-1..-1/trackData.json"
        String refererLoc = "{\"padding\":0, \"projection\":\"None\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"Group11.4\"}], \"label\":\"Group11.4\"}:-1..-1:1..16607"

        when: "we get the projected track data "
        JSONObject trackObject = trackService.projectTrackData(sequenceStrings, dataFileName, refererLoc, Organism.first())

        then: "we expect to get sane results"
        assert trackObject.featureCount == 6
        def minStart = trackObject.minStart
        def maxEnd = trackObject.maxEnd
        JSONArray nclist = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)
        assert nclist.size() == 5

        // the next array should go somewhere completely else
        JSONArray firstLastArray = nclist.getJSONArray(0)
        assert firstLastArray.getInt(1) == 10257// end of the first set
//        assert firstLastArray.getInt(2)==185696+45575// end of the first set
        assert firstLastArray.getInt(2) == 18596// end of the first set

        JSONArray lastLastArray = nclist.getJSONArray(4)
        assert lastLastArray.getInt(1) == 62507// end of the last set
        assert lastLastArray.getInt(2) == 64197// end of the last set
    }

    /**
     *  Group11.4: Projected: 0,2538 <-> 14574,15734  (5 groups), Unprojected: 10257,18596 (first) 62507,64197 (last)
     *
     */
    @Ignore  // we are not handling Exon type projectinos
    void "Projected 11.4 individually"() {
        given: "proper inputs"
        JSONArray sequenceStrings = new JSONArray('[{name:"Group11.4"}]')
        String dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/{\"padding\":0, \"projection\":\"Exon\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"Group11.4\"}], \"label\":\"Group11.4\"}:-1..-1/trackData.json"
        String refererLoc = "{\"padding\":0, \"projection\":\"Exon\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"Group11.4\"}], \"label\":\"Group11.4\"}:-1..-1:1..16607"

        when: "we get the projected track data "
        JSONObject trackObject = trackService.projectTrackData(sequenceStrings, dataFileName, refererLoc, Organism.first())

        then: "we expect to get sane results"
        assert trackObject.featureCount == 6
        def minStart = trackObject.minStart
        def maxEnd = trackObject.maxEnd
        JSONArray nclist = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)
        assert nclist.size() == 5

        // the next array should go somewhere completely else
        JSONArray firstLastArray = nclist.getJSONArray(0)
        assert firstLastArray.getInt(1) == 0// end of the first set
//        assert firstLastArray.getInt(2)==185696+45575// end of the first set
//        assert firstLastArray.getInt(2) == 2538 - 8 // end of the first set - 9 exons (8 intermediate
        assert firstLastArray.getInt(2) == 2538  // end of the first set - 9 exons (8 intermediate

        JSONArray lastLastArray = nclist.getJSONArray(4)
        assert lastLastArray.getInt(1) == 14574// end of the last set
        assert lastLastArray.getInt(2) == 15734// end of the last set
    }

    /**
     *  Group11.4: Projected: 0,2538 <-> 14574,15734  (5 groups), Unprojected: 10257,18596 (first) 62507,64197 (last)
     *
     */
    @Ignore  // we are not handling Exon type projectinos
    void "Projected 11.4 individually with padding"() {
        given: "proper inputs"
        JSONArray sequenceStrings = new JSONArray('[{name:"Group11.4"}]')
        Integer padding = 20
        String dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/{\"padding\": ${padding}, \"projection\":\"Exon\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"Group11.4\"}], \"label\":\"Group11.4\"}:-1..-1/trackData.json"
        String refererLoc = "{\"padding\":${padding}, \"projection\":\"Exon\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"Group11.4\"}], \"label\":\"Group11.4\"}:-1..-1:1..16607"

        when: "we get the projected track data "
        JSONObject trackObject = trackService.projectTrackData(sequenceStrings, dataFileName, refererLoc, Organism.first())

        then: "we expect to get sane results"
        assert trackObject.featureCount == 6
        def minStart = trackObject.minStart
        def maxEnd = trackObject.maxEnd
        JSONArray nclist = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)
        assert nclist.size() == 5

        // the next array should go somewhere completely else
        JSONArray firstLastArray = nclist.getJSONArray(0)
        assert firstLastArray.getInt(1) == 0 + padding // end of the first set
        assert firstLastArray.getInt(2) == 2538 + (padding + (padding * (8 * 2))) // end of the first set

        JSONArray lastLastArray = nclist.getJSONArray(4)
        Integer paddingCount = 55
        assert lastLastArray.getInt(1) == 14574 + (padding * paddingCount)// end of the last set
        assert lastLastArray.getInt(2) == 15734 + (padding * (paddingCount + 6))// end of the last set . ..  including exons
    }

    /**
     *
     *  GroupUn87: Projected: 0,213 <-> 718,838   (4 groups), Unprojected: 9966,10179 (first)  45455,45575 (last)
     *  Group11.4: Projected: 0,2538 <-> 14574,15734  (5 groups), Unprojected: 10257,18596 (first) 62507,64197 (last)
     */
    @Ignore  // we are not handling Exon type projectinos
    void "exon projections of contiguous tracks should work"() {

        given: "proper inputs"
        JSONArray sequenceStrings = new JSONArray('[{name:"GroupUn87"}, {name:"Group11.4"}]')
        String dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/{\"padding\":0, \"projection\":\"Exon\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"GroupUn87\"},{\"name\":\"Group11.4\"}], \"label\":\"GroupUn87::Group11.4\"}:-1..-1/trackData.json"
        String refererLoc = "{\"padding\":0, \"projection\":\"Exon\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"GroupUn87\"},{\"name\":\"Group11.4\"}], \"label\":\"GroupUn87::Group11.4\"}:-1..-1:1..16607"

        when: "we get the projected track data "
        JSONObject trackObject = trackService.projectTrackData(sequenceStrings, dataFileName, refererLoc, Organism.first())

        then: "we expect to get sane results"
        assert trackObject.featureCount == 10
        def minStart = trackObject.minStart
        def maxEnd = trackObject.maxEnd
        JSONArray nclist = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)
        assert nclist.size() == 9
        JSONArray firstArray = nclist.getJSONArray(0)
        assert firstArray.getInt(1) == 0
        assert firstArray.getInt(2) == 213

        JSONArray lastFirstArray = nclist.getJSONArray(3)
        assert lastFirstArray.getInt(1) == 718
        assert lastFirstArray.getInt(2) == 838  // end of the first set

        // the next array should start at the end of thast one
        JSONArray firstLastArray = nclist.getJSONArray(4)
        assert firstLastArray.getInt(1) == 0 + 838
        assert firstLastArray.getInt(2) == 2538 + 838

        JSONArray lastLastArray = nclist.getJSONArray(8)
        assert lastLastArray.getInt(1) == 14574 + 838
        assert lastLastArray.getInt(2) == 15734 + 838
    }

    /**
     *
     *  GroupUn87: Projected: 0,213 <-> 718,838   (4 groups), Unprojected: 9966,10179 (first)  45455,45575 (last)
     *  Group11.4: Projected: 0,2538 <-> 14574,15734  (5 groups), Unprojected: 10257,18596 (first) 62507,64197 (last)
     */
    @Ignore  // we are not handling Exon type projectinos
    void "exon projections of contiguous tracks should work with padding"() {

        given: "proper inputs"
        JSONArray sequenceStrings = new JSONArray('[{name:"GroupUn87"}, {name:"Group11.4"}]')
        Integer padding = 20
        String dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/{\"padding\":${padding}, \"projection\":\"Exon\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"GroupUn87\"},{\"name\":\"Group11.4\"}], \"label\":\"GroupUn87::Group11.4\"}:-1..-1/trackData.json"
        String refererLoc = "{\"padding\":${padding}, \"projection\":\"Exon\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"GroupUn87\"},{\"name\":\"Group11.4\"}], \"label\":\"GroupUn87::Group11.4\"}:-1..-1:1..16607"

        when: "we get the projected track data "
        JSONObject trackObject = trackService.projectTrackData(sequenceStrings, dataFileName, refererLoc, Organism.first())

        then: "we expect to get sane results"
        assert trackObject.featureCount == 10
        def minStart = trackObject.minStart
        def maxEnd = trackObject.maxEnd
        JSONArray nclist = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)
        assert nclist.size() == 9
        JSONArray firstArray = nclist.getJSONArray(0)
        Integer paddingCount = 1
        assert firstArray.getInt(1) == 0 + padding * paddingCount
        assert firstArray.getInt(2) == 213 + padding * paddingCount

        when: "adjust "
        paddingCount += 10

        then:
        JSONArray lastFirstArray = nclist.getJSONArray(3)
//        assert lastFirstArray.getInt(1) == 718 + padding * paddingCount // end of first set
        assert lastFirstArray.getInt(1) == 718 + padding * paddingCount // end of first set
        assert lastFirstArray.getInt(2) == 838 + padding * paddingCount // end of the first set

        // the next array should start at the end of thast one
        when: "adjust "
        paddingCount += 1

        then:
        JSONArray firstLastArray = nclist.getJSONArray(4)
        assert firstLastArray.getInt(1) == 0 + 838 + padding * paddingCount
        assert firstLastArray.getInt(2) == 2538 + 838 + padding * (paddingCount+16)

        when: "adjust "
        paddingCount += 16 + 2 + 36 //?

        then:
        JSONArray lastLastArray = nclist.getJSONArray(8)
        assert lastLastArray.getInt(1) == 14574 + 838 + padding * paddingCount
        assert lastLastArray.getInt(2) == 15734 + 838 + padding * (paddingCount+6)
    }

    /**
     *
     *  Group11.6: Unprojected (2 chunks) first chunk, 6958 <=> 1080855  second chunk, 1083799 <=> 1494475 Unprojected: 9966,10179 (first)  45455,45575 (last)
     *  (lf-1 . . 61 pieces, 6958 <=> 8455 first, 1078032 <=> 1080855 last ) ,
     *  (lf-2 . . 43 pieces, 1083799 <=>  1102753 first, 1494115 <=> 1494475 last ) ,
     *
     *  Projected: (2 chunks) first chunk, 0 <=> 91084 second chunk, 91399 <=> 168772
     *  (lf-1 . . 61 pieces, 0 <=> 1281 first, 89936 <=> 91084 last ) ,
     *  (lf-2 . . 43 pieces, 91399 <=>  95943 first, 169097 <=> 168772 last ) ,
     *
     * TODO: // look at this
     *  Group1.10: Unprojected (3 chunks)
     *  first chunk: 19636 <=>  588668  second chunk: 588729 <=> 1267170  third chunk: 1268021 ,1405215 (second)
     *  (lf-1 . . 57 pieces, 19636 <=> 31167 first, 582938 <=> 588668 last ) ,
     *  (lf-2 . . 61 pieces, 588729 <=>  594164 first, 1261785 <=> 1267170 last ) ,
     *  (lf-3 . . 16 pieces, 1268021 <=>  1277382 first, 1389396 <=> 1405215 last ) ,
     *
     *  Group1.10: Projected (3 chunks)
     *  first chunk: 0 <=> 108503 second chunk: 108504 <=> 201343 third chunk: 201344 ,230587
     *  (lf-1 . . 57 pieces, 0 <=> 874 first, 107145 <=> 108503 last ) ,
     *  (lf-2 . . 61 pieces, 108504 <=> 109549  first, 195958 <=> 201343 last ) ,
     *  (lf-3 . . 16 pieces, 201344 <=>  206511 first, 227803 <=> 230587 last ) ,
     */
    @Ignore  // we are not handling Exon type projectinos
    void "chunking / chunking projection"() {

        given: "proper inputs"
        JSONArray sequenceStrings = new JSONArray('[{name:"Group11.6"},{name:"Group1.10"}]')
        String trackName = "Official Gene Set v3.2"
        String dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/${trackName}/{\"padding\":0, \"projection\":\"Exon\", \"referenceTrack\":\"Official Gene Set v3.2\", \"sequenceList\":[{\"name\":\"${sequenceStrings[0]}\"},{\"name\":\"${sequenceStrings[1]}\"}], \"label\":\"${sequenceStrings.join('::')}\"}:-1..-1/trackData.json"
        String chunk1 = "test/integration/resources/sequences/honeybee-tracks/tracks/${trackName}/${sequenceStrings[0]}/lf-1.json"
        String chunk2 = "test/integration/resources/sequences/honeybee-tracks/tracks/${trackName}/${sequenceStrings[0]}/lf-2.json"
        String chunk3 = "test/integration/resources/sequences/honeybee-tracks/tracks/${trackName}/${sequenceStrings[1]}/lf-1.json"
        // don't need to test chunk5 as well
        String chunk5 = "test/integration/resources/sequences/honeybee-tracks/tracks/${trackName}/${sequenceStrings[1]}/lf-3.json"
        String refererLoc = "{\"padding\":0, \"projection\":\"Exon\", \"referenceTrack\":\"${trackName}\", \"sequenceList\":[{\"name\":\"${sequenceStrings[0]}\"},{\"name\":\"${sequenceStrings[1]}\"}], \"label\":\"${sequenceStrings.join('::')}\"}:-1..-1:1..16607"
        JSONArray array

        when: "we get the projected track data "
        JSONObject trackObject = trackService.projectTrackData(sequenceStrings, dataFileName, refererLoc, Organism.first())

        then: "we expect to get sane results"
        JSONArray nclist = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)
        assert nclist.size() == 5

        when:
        array = nclist.getJSONArray(0)

        then:
        assert array.getInt(0) == 4 // it is a chunk
        assert array.getInt(1) == 0
        assert array.getInt(2) == 91084

        when:
        array = nclist.getJSONArray(1)

        then:
        assert array.getInt(0) == 4
        assert array.getInt(1) == 91084
        assert array.getInt(2) == 168772 // end of the first set

        when:
        array = nclist.getJSONArray(2)

        then:
        assert array.getInt(0) == 4
        assert array.getInt(1) == 0 + 168772
        assert array.getInt(2) == 108166 + 168772

        when:
        array = nclist.getJSONArray(3)

        then:
        assert array.getInt(0) == 4
        assert array.getInt(1) == 108166  + 168772
        assert array.getInt(2) == 200683 + 168772

        when:
        array = nclist.getJSONArray(4)

        then:
        assert array.getInt(0) == 4
        assert array.getInt(1) == 200683 + 168772
        assert array.getInt(2) == 229828 + 168772

        when: "we load the first of the chunk data"
        JSONArray chunk1Data = trackService.loadChunkData(chunk1, refererLoc, Organism.first(), 0,trackName)
        array = chunk1Data.getJSONArray(0)

        then: "confirm that chunk 1 is projected "
        assert chunk1Data.size() == 61
        assert array.getInt(1) == 0
        assert array.getInt(2) == 1278

        when:
        array = chunk1Data.getJSONArray(60)

        then:
        assert array.getInt(1) == 89628
        assert array.getInt(2) == 91084


        when: "we load the second chunk"
        JSONArray chunk2Data = trackService.loadChunkData(chunk2, refererLoc, Organism.first(), 0,trackName)
        array = chunk2Data.getJSONArray(0)

        then: "we should get the properly projected chunks for 2"
        assert array.getInt(1) == 91084
        assert array.getInt(2) == 95607

        when:
        array = chunk2Data.getJSONArray(chunk2Data.size() - 1)

        then:
        assert array.getInt(1) == 168511
        assert array.getInt(2) == 168772

        when: "we load the third chunk using the offset from previous sequence group"
        JSONArray chunk3Data = trackService.loadChunkData(chunk3, refererLoc, Organism.first(), 168772,trackName)
        array = chunk3Data.getJSONArray(0)

        then: "confirm that chunk 3 is projected "
        assert chunk3Data.size() == 57
        assert array.getInt(1) == 0 + 168772
        assert array.getInt(2) == 873 + 168772

        when:
        array = chunk3Data.getJSONArray(56)

        then:
        assert array.getInt(1) == 106813 + 168772
        assert array.getInt(2) == 108166 + 168772

        when: "we load the last chunk using the offset from previous sequence group"
//        *  (lf-3 . . 16 pieces, 201344 <=>  206511 first, 227803 <=> 230587 last ) ,
        JSONArray chunk5Data = trackService.loadChunkData(chunk5, refererLoc, Organism.first(), 168772,trackName)
        array = chunk5Data.getJSONArray(0)

        then: "confirm that chunk 5 is projected "
        assert chunk5Data.size() == 15
        assert array.getInt(1) == 200683 + 168772
        assert array.getInt(2) == 205830 + 168772

        when:
        array = chunk5Data.getJSONArray(chunk5Data.size() - 1)

        then:
        assert array.getInt(1) == 227055 + 168772
        assert array.getInt(2) == 229828 + 168772
    }

    void "test sanitizeCoordinateArray method"() {

        given: "a user, organism, and group"
        User user = User.first()
        Organism organism = Organism.first()
        UserGroup group = UserGroup.first()
        String trackName = "Official Gene Set v3.2"

        // top-level feature has -1 coordinates
        String payloadOneString = "[[0,-1,-1,-1,\"amel_OGSv3.2\",\"Group1.1\",\"GB42155-RA\",1,\"GB42155-RA\",\"mRNA\",[[1,38227,38597,-1,\"amel_OGSv3.2\",\"Group1.1\",0,\"CDS\"],[1,37711,38226,-1,\"amel_OGSv3.2\",\"Group1.1\",2,\"CDS\"],[2,38628,38907,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"five_prime_UTR\"],[2,38597,38627,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"five_prime_UTR\"],[2,37229,37711,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"three_prime_UTR\"],[2,35285,37228,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"three_prime_UTR\"],[2,35285,37228,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,37229,38226,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,38227,38627,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,38628,38907,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"]],{\"Sublist\":[[0,509862,511494,1,\"amel_OGSv3.2\",\"Group1.1\",\"GB42176-RA\",0.999828,\"GB42176-RA\",\"mRNA\",[[1,510317,510370,1,\"amel_OGSv3.2\",\"Group1.1\",0,\"CDS\"],[1,510467,510572,1,\"amel_OGSv3.2\",\"Group1.1\",1,\"CDS\"],[1,510695,510755,1,\"amel_OGSv3.2\",\"Group1.1\",1,\"CDS\"],[1,510948,511213,1,\"amel_OGSv3.2\",\"Group1.1\",1,\"CDS\"],[1,511377,511494,1,\"amel_OGSv3.2\",\"Group1.1\",0,\"CDS\"],[2,509862,510161,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"five_prime_UTR\"],[2,510289,510317,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"five_prime_UTR\"],[2,509862,510161,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510289,510370,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510467,510572,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510695,510755,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510948,511213,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,511377,511494,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"]]]]}]]"
        String sanitizedPayloadOneString = "[]"

        // 2 sub-features have -1 coordinates
        String payloadTwoString = "[[0,35285,38907,-1,\"amel_OGSv3.2\",\"Group1.1\",\"GB42155-RA\",1,\"GB42155-RA\",\"mRNA\",[[1,38227,38597,-1,\"amel_OGSv3.2\",\"Group1.1\",0,\"CDS\"],[1,37711,38226,-1,\"amel_OGSv3.2\",\"Group1.1\",2,\"CDS\"],[2,-1,-1,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"five_prime_UTR\"],[2,38597,38627,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"five_prime_UTR\"],[2,37229,37711,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"three_prime_UTR\"],[2,35285,37228,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"three_prime_UTR\"],[2,35285,37228,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,37229,38226,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,38227,38627,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,-1,-1,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"]],{\"Sublist\":[[0,509862,511494,1,\"amel_OGSv3.2\",\"Group1.1\",\"GB42176-RA\",0.999828,\"GB42176-RA\",\"mRNA\",[[1,510317,510370,1,\"amel_OGSv3.2\",\"Group1.1\",0,\"CDS\"],[1,510467,510572,1,\"amel_OGSv3.2\",\"Group1.1\",1,\"CDS\"],[1,510695,510755,1,\"amel_OGSv3.2\",\"Group1.1\",1,\"CDS\"],[1,510948,511213,1,\"amel_OGSv3.2\",\"Group1.1\",1,\"CDS\"],[1,511377,511494,1,\"amel_OGSv3.2\",\"Group1.1\",0,\"CDS\"],[2,509862,510161,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"five_prime_UTR\"],[2,510289,510317,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"five_prime_UTR\"],[2,509862,510161,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510289,510370,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510467,510572,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510695,510755,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510948,511213,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,511377,511494,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"]]]]}]]"
        String sanitizedPayloadTwoString = "[[0,35285,38907,-1,\"amel_OGSv3.2\",\"Group1.1\",\"GB42155-RA\",1,\"GB42155-RA\",\"mRNA\",[[1,38227,38597,-1,\"amel_OGSv3.2\",\"Group1.1\",0,\"CDS\"],[1,37711,38226,-1,\"amel_OGSv3.2\",\"Group1.1\",2,\"CDS\"],[2,38597,38627,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"five_prime_UTR\"],[2,37229,37711,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"three_prime_UTR\"],[2,35285,37228,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"three_prime_UTR\"],[2,35285,37228,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,37229,38226,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,38227,38627,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"]],{\"Sublist\":[[0,509862,511494,1,\"amel_OGSv3.2\",\"Group1.1\",\"GB42176-RA\",0.999828,\"GB42176-RA\",\"mRNA\",[[1,510317,510370,1,\"amel_OGSv3.2\",\"Group1.1\",0,\"CDS\"],[1,510467,510572,1,\"amel_OGSv3.2\",\"Group1.1\",1,\"CDS\"],[1,510695,510755,1,\"amel_OGSv3.2\",\"Group1.1\",1,\"CDS\"],[1,510948,511213,1,\"amel_OGSv3.2\",\"Group1.1\",1,\"CDS\"],[1,511377,511494,1,\"amel_OGSv3.2\",\"Group1.1\",0,\"CDS\"],[2,509862,510161,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"five_prime_UTR\"],[2,510289,510317,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"five_prime_UTR\"],[2,509862,510161,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510289,510370,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510467,510572,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510695,510755,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510948,511213,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,511377,511494,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"]]]]}]]"

        // top-level feature in subListColumn has -1 coordinates
        String payloadThreeString = "[[0,35285,38907,-1,\"amel_OGSv3.2\",\"Group1.1\",\"GB42155-RA\",1,\"GB42155-RA\",\"mRNA\",[[1,38227,38597,-1,\"amel_OGSv3.2\",\"Group1.1\",0,\"CDS\"],[1,37711,38226,-1,\"amel_OGSv3.2\",\"Group1.1\",2,\"CDS\"],[2,38628,38907,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"five_prime_UTR\"],[2,38597,38627,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"five_prime_UTR\"],[2,37229,37711,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"three_prime_UTR\"],[2,35285,37228,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"three_prime_UTR\"],[2,35285,37228,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,37229,38226,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,38227,38627,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,38628,38907,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"]],{\"Sublist\":[[0,-1,-1,1,\"amel_OGSv3.2\",\"Group1.1\",\"GB42176-RA\",0.999828,\"GB42176-RA\",\"mRNA\",[[1,510317,510370,1,\"amel_OGSv3.2\",\"Group1.1\",0,\"CDS\"],[1,510467,510572,1,\"amel_OGSv3.2\",\"Group1.1\",1,\"CDS\"],[1,510695,510755,1,\"amel_OGSv3.2\",\"Group1.1\",1,\"CDS\"],[1,510948,511213,1,\"amel_OGSv3.2\",\"Group1.1\",1,\"CDS\"],[1,511377,511494,1,\"amel_OGSv3.2\",\"Group1.1\",0,\"CDS\"],[2,509862,510161,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"five_prime_UTR\"],[2,510289,510317,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"five_prime_UTR\"],[2,509862,510161,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510289,510370,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510467,510572,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510695,510755,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510948,511213,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,511377,511494,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"]]]]}]]"
        String sanitizedPayloadThreeString = "[[0,35285,38907,-1,\"amel_OGSv3.2\",\"Group1.1\",\"GB42155-RA\",1,\"GB42155-RA\",\"mRNA\",[[1,38227,38597,-1,\"amel_OGSv3.2\",\"Group1.1\",0,\"CDS\"],[1,37711,38226,-1,\"amel_OGSv3.2\",\"Group1.1\",2,\"CDS\"],[2,38628,38907,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"five_prime_UTR\"],[2,38597,38627,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"five_prime_UTR\"],[2,37229,37711,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"three_prime_UTR\"],[2,35285,37228,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"three_prime_UTR\"],[2,35285,37228,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,37229,38226,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,38227,38627,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,38628,38907,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"]],{\"Sublist\":[]}]]"

        // 2 sub-features in subListColumn has -1 coordinates
        String payloadFourString = "[[0,35285,38907,-1,\"amel_OGSv3.2\",\"Group1.1\",\"GB42155-RA\",1,\"GB42155-RA\",\"mRNA\",[[1,38227,38597,-1,\"amel_OGSv3.2\",\"Group1.1\",0,\"CDS\"],[1,37711,38226,-1,\"amel_OGSv3.2\",\"Group1.1\",2,\"CDS\"],[2,38628,38907,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"five_prime_UTR\"],[2,38597,38627,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"five_prime_UTR\"],[2,37229,37711,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"three_prime_UTR\"],[2,35285,37228,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"three_prime_UTR\"],[2,35285,37228,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,37229,38226,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,38227,38627,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,38628,38907,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"]],{\"Sublist\":[[0,509862,511494,1,\"amel_OGSv3.2\",\"Group1.1\",\"GB42176-RA\",0.999828,\"GB42176-RA\",\"mRNA\",[[1,510317,510370,1,\"amel_OGSv3.2\",\"Group1.1\",0,\"CDS\"],[1,510467,510572,1,\"amel_OGSv3.2\",\"Group1.1\",1,\"CDS\"],[1,510695,510755,1,\"amel_OGSv3.2\",\"Group1.1\",1,\"CDS\"],[1,510948,511213,1,\"amel_OGSv3.2\",\"Group1.1\",1,\"CDS\"],[1,-1,-1,1,\"amel_OGSv3.2\",\"Group1.1\",0,\"CDS\"],[2,509862,510161,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"five_prime_UTR\"],[2,510289,510317,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"five_prime_UTR\"],[2,509862,510161,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510289,510370,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510467,510572,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510695,510755,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510948,511213,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,-1,-1,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"]]]]}]]"
        String sanitizedPayloadFourString = "[[0,35285,38907,-1,\"amel_OGSv3.2\",\"Group1.1\",\"GB42155-RA\",1,\"GB42155-RA\",\"mRNA\",[[1,38227,38597,-1,\"amel_OGSv3.2\",\"Group1.1\",0,\"CDS\"],[1,37711,38226,-1,\"amel_OGSv3.2\",\"Group1.1\",2,\"CDS\"],[2,38628,38907,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"five_prime_UTR\"],[2,38597,38627,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"five_prime_UTR\"],[2,37229,37711,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"three_prime_UTR\"],[2,35285,37228,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"three_prime_UTR\"],[2,35285,37228,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,37229,38226,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,38227,38627,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"],[2,38628,38907,-1,\"amel_OGSv3.2\",\"Group1.1\",1,\"exon\"]],{\"Sublist\":[[0,509862,511494,1,\"amel_OGSv3.2\",\"Group1.1\",\"GB42176-RA\",0.999828,\"GB42176-RA\",\"mRNA\",[[1,510317,510370,1,\"amel_OGSv3.2\",\"Group1.1\",0,\"CDS\"],[1,510467,510572,1,\"amel_OGSv3.2\",\"Group1.1\",1,\"CDS\"],[1,510695,510755,1,\"amel_OGSv3.2\",\"Group1.1\",1,\"CDS\"],[1,510948,511213,1,\"amel_OGSv3.2\",\"Group1.1\",1,\"CDS\"],[2,509862,510161,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"five_prime_UTR\"],[2,510289,510317,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"five_prime_UTR\"],[2,509862,510161,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510289,510370,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510467,510572,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510695,510755,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"],[2,510948,511213,1,\"amel_OGSv3.2\",\"Group1.1\",0.999828,\"exon\"]]]]}]]"

        when: "we try to sanitize a coordinate JSON array that has a top-level feature with invalid coordinates"
        JSONArray payloadOneArray = JSON.parse(payloadOneString) as JSONArray
        JSONArray payloadOneReturnArray = trackService.sanitizeCoordinateArray(payloadOneArray, organism, trackName)

        then: "we should see an empty coordinate JSON array"
        println "PAYLOAD ONE RETURN ARRAY: ${payloadOneReturnArray.toString()}"
        assert payloadOneReturnArray.size() == 0
        assert payloadOneArray.size() == payloadOneReturnArray.size()

        when: "we try to sanitize a coordinate JSON array that has 2 sub-features with invalid coordinates"
        JSONArray payloadTwoArray = JSON.parse(payloadTwoString) as JSONArray
        JSONArray payloadTwoReturnArray = trackService.sanitizeCoordinateArray(payloadTwoArray, organism, trackName)

        then: "we should see a valid coordinate JSON array without those 2 sub-features"
        println "PAYLOAD TWO RETURN ARRAY: ${payloadTwoReturnArray.toString()}"
        assert payloadTwoReturnArray.toString() == sanitizedPayloadTwoString
        assert payloadTwoArray.size() == payloadTwoReturnArray.size()

        when: "we try to sanitize a coordinate JSON array that has a subList whose top-level feature has invalid coordinates"
        JSONArray payloadThreeArray = JSON.parse(payloadThreeString) as JSONArray
        JSONArray payloadThreeReturnArray = trackService.sanitizeCoordinateArray(payloadThreeArray, organism, trackName)

        then:" we should see a valid coordinate JSON array with an empty subList"
        println "PAYLOAD THREE RETURN ARRAY: ${payloadThreeReturnArray.toString()}"
        assert payloadThreeReturnArray.toString() == sanitizedPayloadThreeString
        assert payloadThreeArray.size() == payloadThreeReturnArray.size()

        when: "we try to sanitize a coordinate JSON array that has a subList whose sub-features have invalid coordinates"
        JSONArray payloadFourArray = JSON.parse(payloadFourString) as JSONArray
        JSONArray payloadFourReturnArray = trackService.sanitizeCoordinateArray(payloadFourArray, organism, trackName)

        then: "we should see a valid coordinate JSON array that has a subList without those sub-features"
        println "PAYLOAD FOUR RETURN ARRAY: ${payloadFourReturnArray.toString()}"
        assert payloadFourReturnArray.toString() == sanitizedPayloadFourString
        assert payloadFourArray.size() == payloadFourReturnArray.size()
    }

    void "project tracks A1, A2, B1"(){

        given: "proper inputs"
        String sequenceList = '[{"name":"Group11.4", "start":52653, "end":59162, "feature":{"name":"GB52236-RA"}},{"name":"Group11.4", "start":10057, "end":18796, "feature":{"name":"GB52238-RA"}},{"name":"GroupUn87", "start":10311, "end":26919, "feature":{"name":"GB53497-RA"}}]'
        String refererLoc = "{\"name\":\"GB52236-RA (Group11.4)::GB52238-RA (Group11.4)::GB53497-RA (GroupUn87)\", \"padding\":0, \"start\":52853, \"end\":104277, \"sequenceList\":${sequenceList}}:52853..104277"
        String location = ":52853..104277"
        JSONArray sequenceStrings = new JSONArray(sequenceList)
        String dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/${refererLoc}${location}/trackData.json"

        when: "we get the projected track data "
        JSONObject trackObject = trackService.projectTrackData(sequenceStrings, dataFileName, refererLoc, Organism.first())
//
        then: "we expect to get sane results"
        assert trackObject.featureCount == 10G
        assert trackObject.intervals.nclist.size() == 3
        assert trackObject.intervals.nclist.getJSONArray(0).getInt(1)==200+ MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH
        assert trackObject.intervals.nclist.getJSONArray(0).getInt(2)==6309+ MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH
        assert trackObject.intervals.nclist.getJSONArray(1).getInt(1)==6709 + MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH
        assert trackObject.intervals.nclist.getJSONArray(1).getInt(2)==15048+ MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH
        assert trackObject.intervals.nclist.getJSONArray(2).getInt(1)==15194+ MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH
        assert trackObject.intervals.nclist.getJSONArray(2).getInt(2)==31402+ MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH

    }

    void "project tracks B1, A1, A2"(){

        given: "proper inputs"
        String sequenceList = "[{\"name\":\"GroupUn87\", \"start\":10311, \"end\":26919, \"feature\":{\"name\":\"GB53497-RA\"}},{\"name\":\"Group11.4\", \"start\":10057, \"end\":18796, \"feature\":{\"name\":\"GB52238-RA\"}},{\"name\":\"Group11.4\", \"start\":52653, \"end\":59162, \"feature\":{\"name\":\"GB52236-RA\"}}]"
        String refererLoc = "{\"id\":39616, \"name\":\"GB53497-RA (GroupUn87)::GB52238-RA (Group11.4)::GB52236-RA (Group11.4)\", \"padding\":0, \"start\":10511, \"end\":104277, \"sequenceList\":${sequenceList}}:10511..104277"
        String location = ":1..31856"
        JSONArray sequenceStrings = new JSONArray(sequenceList)
        String dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/${refererLoc}${location}/trackData.json"

        when: "we get the projected track data "
        JSONObject trackObject = trackService.projectTrackData(sequenceStrings, dataFileName, refererLoc, Organism.first())
//
        then: "we expect to get sane results"
        assert trackObject.featureCount == 10
        assert trackObject.intervals.nclist.size() == 3
        assert trackObject.intervals.nclist.getJSONArray(0).getInt(1)==200
        assert trackObject.intervals.nclist.getJSONArray(0).getInt(2)==16408
        assert trackObject.intervals.nclist.getJSONArray(1).getInt(1)==16808
        assert trackObject.intervals.nclist.getJSONArray(1).getInt(2)==25147
        assert trackObject.intervals.nclist.getJSONArray(2).getInt(1)==25547 + org.bbop.apollo.gwt.shared.projection.MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH
        assert trackObject.intervals.nclist.getJSONArray(2).getInt(2)==31656+ org.bbop.apollo.gwt.shared.projection.MultiSequenceProjection.DEFAULT_SCAFFOLD_BORDER_LENGTH

    }

    void "get two large scaffold chunks, 1.10::11.6"(){

        given: "proper input"
        String sequenceList = "[{\"name\":\"Group1.10\",\"start\":0,\"end\":1405242,\"reverse\":false},{\"name\":\"Group11.6\",\"start\":0,\"end\":1566327,\"reverse\":false}]"
        String refererLoc= "{\"sequenceList\":${sequenceList}}"
        String location = ":2516297..1566327"
        String dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/${refererLoc}${location}/trackData.json"
        JSONArray sequenceArray = new JSONArray(sequenceList)

        when: "we ingest the data"
        println "# of sequences ${Sequence.count}"
        JSONObject trackObject = trackService.projectTrackData(sequenceArray, dataFileName, refererLoc, Organism.first())

        then: "we expect stuff not to blow up"
        assert trackObject != null

        when: "when we get the nclist"
        JSONArray nclistArray = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)

        then: "we expect the start and the stop to be in order "
        assert nclistArray.size()==5
        assert nclistArray[0][1] < nclistArray[0][2]
        assert nclistArray[0][2] < nclistArray[1][1]
        assert nclistArray[1][1] < nclistArray[1][2]
        assert nclistArray[1][2] < nclistArray[2][1]
        assert nclistArray[2][1] < nclistArray[2][2]
        assert nclistArray[2][2] < nclistArray[3][1]
        assert nclistArray[3][1] < nclistArray[3][2]
        assert nclistArray[3][2] < nclistArray[4][1]
        assert nclistArray[4][1] < nclistArray[4][2]

    }

    void "small scaffold should be reversed properly"(){

        given: "a request for 11.4 (small) forward"
        String sequenceList = "[{\"name\":\"Group11.4\",\"start\":0,\"end\":75085,\"reverse\":false}]"
        String refererLoc= "{\"sequenceList\":${sequenceList}}"
        String location = ":0..1000"
        String dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/${refererLoc}${location}/trackData.json"
        JSONArray sequenceArray = new JSONArray(sequenceList)

        when: "we create a forward request for the 11.4"
        println "# of sequences ${Sequence.count}"
        JSONObject trackObject = trackService.projectTrackData(sequenceArray, dataFileName, refererLoc, Organism.first())

        then: "we should get results"
        assert trackObject != null

        when: "we grab the data"
        JSONArray nclistArray = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)
        println nclistArray[1][2] + " vs " + nclistArray[2][1]

        then: "we expect the order to be correct"
        assert nclistArray.size()==5
        assert nclistArray[0][1] < nclistArray[0][2]
        assert nclistArray[0][2] < nclistArray[1][1]
        assert nclistArray[1][1] < nclistArray[1][2]
        assert nclistArray[1][2] > nclistArray[2][1]
        assert nclistArray[2][1] < nclistArray[2][2]
        assert nclistArray[2][2] < nclistArray[3][1]
        assert nclistArray[3][1] < nclistArray[3][2]
        assert nclistArray[3][2] < nclistArray[4][1]
        assert nclistArray[4][1] < nclistArray[4][2]

        when: "We reverse it"
        sequenceList = "[{\"name\":\"Group11.4\",\"start\":0,\"end\":75085,\"reverse\":true}]"
        refererLoc= "{\"sequenceList\":${sequenceList}}"
        dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/${refererLoc}${location}/trackData.json"
        sequenceArray = new JSONArray(sequenceList)
        println "# of sequences ${Sequence.count}"
        trackObject = trackService.projectTrackData(sequenceArray, dataFileName, refererLoc, Organism.first())

        then: "we should get results"
        assert trackObject != null

        when: "we grab the data"
        nclistArray = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)

        then: "we expect the order to be correct"
        assert nclistArray.size()==5
        assert nclistArray[0][1] < nclistArray[0][2]
        assert nclistArray[0][2] < nclistArray[1][1]
        assert nclistArray[1][1] < nclistArray[1][2]
        assert nclistArray[1][2] < nclistArray[2][1]
        assert nclistArray[2][1] < nclistArray[2][2]
        assert nclistArray[2][2] > nclistArray[3][1]
        assert nclistArray[3][1] < nclistArray[3][2]
        assert nclistArray[3][2] < nclistArray[4][1]
        assert nclistArray[4][1] < nclistArray[4][2]
    }

    void "large scaffold should be reversed properly"(){

        given: "a request for 1.10 (large) forward"
        String sequenceList = "[{\"name\":\"Group1.10\",\"start\":0,\"end\":1405242,\"reverse\":false}]"
        String refererLoc= "{\"sequenceList\":${sequenceList}}"
        String location = ":0..1000"
        String dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/${refererLoc}${location}/trackData.json"
        JSONArray sequenceArray = new JSONArray(sequenceList)

        when: "we create a forward request for the 1.10"
        println "# of sequences ${Sequence.count}"
        JSONObject trackObject = trackService.projectTrackData(sequenceArray, dataFileName, refererLoc, Organism.first())

        then: "we should get results"
        assert trackObject != null

        when: "we grab the data"
        JSONArray nclistArray = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)

        then: "we expect the order to be correct"
        assert nclistArray.size()==3
        assert nclistArray[0][1] < nclistArray[0][2]
        assert nclistArray[0][2] < nclistArray[1][1]
        assert nclistArray[1][1] < nclistArray[1][2]
        assert nclistArray[1][2] < nclistArray[2][1]
        assert nclistArray[2][1] < nclistArray[2][2]

        when: "We reverse it"
        sequenceList = "[{\"name\":\"Group1.10\",\"start\":0,\"end\":1405242,\"reverse\":true}]"
        refererLoc= "{\"sequenceList\":${sequenceList}}"
        dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/${refererLoc}${location}/trackData.json"
        sequenceArray = new JSONArray(sequenceList)
        println "# of sequences ${Sequence.count}"
        trackObject = trackService.projectTrackData(sequenceArray, dataFileName, refererLoc, Organism.first())

        then: "we should get results"
        assert trackObject != null

        when: "we grab the data"
        nclistArray = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)

        then: "we expect the order to be correct"
        assert nclistArray.size()==3
        assert nclistArray[0][1] < nclistArray[0][2]
        assert nclistArray[0][2] < nclistArray[1][1]
        assert nclistArray[1][1] < nclistArray[1][2]
        assert nclistArray[1][2] < nclistArray[2][1]
        assert nclistArray[2][1] < nclistArray[2][2]
    }

    void "get two small scaffolds, 11.4,Un87"(){

        given: "proper 11.4 and Un87, should go the duration, though if we reverse 11.4, it should still go the length (beyond the length of the first one)"
        // TODO: set this properly
        String sequenceList = "[{\"name\":\"Group11.4\",\"start\":0,\"end\":75085,\"reverse\":false},{\"name\":\"GroupUn87\",\"start\":0,\"end\":78258,\"reverse\":false}]"
        String refererLoc= "{\"sequenceList\":${sequenceList}}"
        String location = ":0..1000"
        String dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/${refererLoc}${location}/trackData.json"
        JSONArray sequenceArray = new JSONArray(sequenceList)

        when: "we ingest the data"
        JSONObject trackObject = trackService.projectTrackData(sequenceArray, dataFileName, refererLoc, Organism.first())

        then: "we expect stuff not to blow up"
        assert trackObject != null

        when: "when we get the nclist"
        JSONArray nclistArray = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)

        then: "we expect the start and the stop to be in order "
        assert nclistArray.size()==9
        assert nclistArray[0][1] < nclistArray[1][1]
        assert nclistArray[0][5] == "Group11.4"
        assert nclistArray[1][1] < nclistArray[2][1]
        assert nclistArray[1][5] == "Group11.4"
        assert nclistArray[2][1] < nclistArray[3][1]
        assert nclistArray[2][5] == "Group11.4"
        assert nclistArray[3][1] < nclistArray[4][1]
        assert nclistArray[3][5] == "Group11.4"
        assert nclistArray[4][1] < nclistArray[5][1]
        assert nclistArray[4][5] == "Group11.4"
        assert nclistArray[5][1] < nclistArray[6][1]
        assert nclistArray[5][5] == "GroupUn87"
        assert nclistArray[6][1] < nclistArray[7][1]
        assert nclistArray[6][5] == "GroupUn87"
        assert nclistArray[7][1] < nclistArray[8][1]
        assert nclistArray[7][5] == "GroupUn87"
        assert nclistArray[8][5] == "GroupUn87"


        when: "we reverse the next one"
        sequenceList = "[{\"name\":\"Group11.4\",\"start\":0,\"end\":75085,\"reverse\":true},{\"name\":\"GroupUn87\",\"start\":0,\"end\":78258,\"reverse\":false}]"
        refererLoc= "{\"sequenceList\":${sequenceList}}"
        dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/${refererLoc}${location}/trackData.json"
        sequenceArray = new JSONArray(sequenceList)
        trackObject = trackService.projectTrackData(sequenceArray, dataFileName, refererLoc, Organism.first())
        nclistArray = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)

        then: "we expect the start and the stop to be in order "
        assert nclistArray.size()==9
        assert nclistArray[0][1] < nclistArray[1][1]
        assert nclistArray[0][5] == "Group11.4"
        assert nclistArray[1][1] < nclistArray[2][1]
        assert nclistArray[1][5] == "Group11.4"
        assert nclistArray[2][1] < nclistArray[3][1]
        assert nclistArray[2][5] == "Group11.4"
        assert nclistArray[3][1] < nclistArray[4][1]
        assert nclistArray[3][5] == "Group11.4"
        assert nclistArray[4][1] < nclistArray[5][1]
        assert nclistArray[4][5] == "Group11.4"
        assert nclistArray[5][1] < nclistArray[6][1]
        assert nclistArray[5][5] == "GroupUn87"
        assert nclistArray[6][1] < nclistArray[7][1]
        assert nclistArray[6][5] == "GroupUn87"
        assert nclistArray[7][1] < nclistArray[8][1]
        assert nclistArray[7][5] == "GroupUn87"
        assert nclistArray[8][5] == "GroupUn87"

        when: "we reverse both"
        sequenceList = "[{\"name\":\"Group11.4\",\"start\":0,\"end\":75085,\"reverse\":true},{\"name\":\"GroupUn87\",\"start\":0,\"end\":78258,\"reverse\":true}]"
        refererLoc= "{\"sequenceList\":${sequenceList}}"
        dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/${refererLoc}${location}/trackData.json"
        sequenceArray = new JSONArray(sequenceList)
        trackObject = trackService.projectTrackData(sequenceArray, dataFileName, refererLoc, Organism.first())
        nclistArray = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)

        then: "we expect the start and the stop to be in order "
        assert nclistArray.size()==9
        assert nclistArray[0][1] < nclistArray[1][1]
        assert nclistArray[0][5] == "Group11.4"
        assert nclistArray[1][1] < nclistArray[2][1]
        assert nclistArray[1][5] == "Group11.4"
        assert nclistArray[2][1] < nclistArray[3][1]
        assert nclistArray[2][5] == "Group11.4"
        assert nclistArray[3][1] < nclistArray[4][1]
        assert nclistArray[3][5] == "Group11.4"
        assert nclistArray[4][1] < nclistArray[5][1]
        assert nclistArray[4][5] == "Group11.4"
        assert nclistArray[5][1] < nclistArray[6][1]
        assert nclistArray[5][5] == "GroupUn87"
        assert nclistArray[6][1] < nclistArray[7][1]
        assert nclistArray[6][5] == "GroupUn87"
        assert nclistArray[7][1] < nclistArray[8][1]
        assert nclistArray[7][5] == "GroupUn87"
        assert nclistArray[8][5] == "GroupUn87"
    }

    void "get two large scaffolds, 1.10::11.6 we should be able to reverse the first one and still have it extend properly"(){

        given: "proper input"
        String sequenceList = "[{\"name\":\"Group1.10\",\"start\":0,\"end\":1405242,\"reverse\":false},{\"name\":\"Group11.6\",\"start\":0,\"end\":1566327,\"reverse\":false}]"
        String refererLoc= "{\"sequenceList\":${sequenceList}}"
        String location = ":2516297..1566327"
        String dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/${refererLoc}${location}/trackData.json"
        JSONArray sequenceArray = new JSONArray(sequenceList)

        when: "we project the data"
        JSONObject trackObject = trackService.projectTrackData(sequenceArray, dataFileName, refererLoc, Organism.first())
        JSONArray nclistArray = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)

        then: "we expect the start and the stop to be in order "
        assert nclistArray.size()==5
        assert nclistArray[0][1] < nclistArray[1][1]
        assert nclistArray[1][1] < nclistArray[2][1]
        assert nclistArray[2][1] < nclistArray[3][1]
        assert nclistArray[3][1] < nclistArray[4][1]

        when: "we reverse the next one"
        sequenceList = "[{\"name\":\"Group1.10\",\"start\":0,\"end\":1405242,\"reverse\":true},{\"name\":\"Group11.6\",\"start\":0,\"end\":1566327,\"reverse\":false}]"
        refererLoc= "{\"sequenceList\":${sequenceList}}"
        dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/${refererLoc}${location}/trackData.json"
        sequenceArray = new JSONArray(sequenceList)
        trackObject = trackService.projectTrackData(sequenceArray, dataFileName, refererLoc, Organism.first())
        nclistArray = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)


        then: "same set of values, and we are still in both scaffolds"
        assert nclistArray.size()==5
        assert nclistArray[0][1] < nclistArray[1][1]
        assert nclistArray[1][1] < nclistArray[2][1]
        assert nclistArray[2][1] < nclistArray[3][1]
        assert nclistArray[3][1] < nclistArray[4][1]

        when: "we reverse both"
        sequenceList = "[{\"name\":\"Group1.10\",\"start\":0,\"end\":1405242,\"reverse\":true},{\"name\":\"Group11.6\",\"start\":0,\"end\":1566327,\"reverse\":true}]"
        refererLoc= "{\"sequenceList\":${sequenceList}}"
        dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/${refererLoc}${location}/trackData.json"
        sequenceArray = new JSONArray(sequenceList)
        trackObject = trackService.projectTrackData(sequenceArray, dataFileName, refererLoc, Organism.first())
        nclistArray = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)


        then: "same set of values, and we are still in both scaffolds"
        assert nclistArray.size()==5
        assert nclistArray[0][1] < nclistArray[1][1]
        assert nclistArray[1][1] < nclistArray[2][1]
        assert nclistArray[2][1] < nclistArray[3][1]
        assert nclistArray[3][1] < nclistArray[4][1]
    }

    void "if we have three small feature scaffolds, they should all be offset correctly"(){

        given: "proper input"
        // String http://localhost:8080/apollo/615463246294435289153778572/jbrowse/data/tracks/Official%20Gene%20Set%20v3.2/%7B%22sequenceList%22:[%7B%22name%22:%22Group11.4%22,%22start%22:10057,%22end%22:18796,%22reverse%22:false,%22feature%22:%7B%22name%22:%22GB52238-RA%22,%22start%22:10057,%22end%22:18796,%22parent_id%22:%22Group11.4%22%7D%7D,%7B%22name%22:%22GroupUn87%22,%22start%22:29196,%22end%22:30529,%22reverse%22:false,%22feature%22:%7B%22name%22:%22GB53498-RA%22,%22start%22:29196,%22end%22:30529,%22parent_id%22:%22GroupUn87%22%7D%7D,%7B%22name%22:%22Group4.1%22,%22start%22:352310,%22end%22:399504,%22reverse%22:false,%22feature%22:%7B%22name%22:%22GB49640-RA%22,%22start%22:352310,%22end%22:399504,%22parent_id%22:%22Group4.1%22%7D%7D]%7D:-1..-1/trackData.json
//        String sequenceList = "[{\"name\":\"Group1.10\",\"start\":0,\"end\":1405242,\"reverse\":false},{\"name\":\"Group11.6\",\"start\":0,\"end\":1566327,\"reverse\":false}]"
        String sequenceList = "[{\"name\":\"Group11.4\",\"start\":10057,\"end\":18796,\"reverse\":false,\"feature\":{\"name\":\"GB52238-RA\",\"start\":10057,\"end\":18796,\"parent_id\":\"Group11.4\"}},{\"name\":\"GroupUn87\",\"start\":29196,\"end\":30529,\"reverse\":false,\"feature\":{\"name\":\"GB53498-RA\",\"start\":29196,\"end\":30529,\"parent_id\":\"GroupUn87\"}},{\"name\":\"Group4.1\",\"start\":352310,\"end\":399504,\"reverse\":false,\"feature\":{\"name\":\"GB49640-RA\",\"start\":352310,\"end\":399504,\"parent_id\":\"Group4.1\"}}]"
        String refererLoc= "{\"sequenceList\":${sequenceList}}"
        String location = ":2516297..1566327"
        String dataFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/Official Gene Set v3.2/${refererLoc}${location}/trackData.json"
        JSONArray sequenceArray = new JSONArray(sequenceList)

        when: "we project the data"
        JSONObject trackObject = trackService.projectTrackData(sequenceArray, dataFileName, refererLoc, Organism.first())
        JSONArray nclistArray = trackObject.getJSONObject(FeatureStringEnum.INTERVALS.value).getJSONArray(FeatureStringEnum.NCLIST.value)

        then: "we expect the start and the stop to be in order and there should be NO overlap"
        assert nclistArray.size()==3
        assert nclistArray[0][1] < nclistArray[0][2]
        assert nclistArray[0][2] < nclistArray[1][1]

        assert nclistArray[1][1] < nclistArray[1][2]
        assert nclistArray[1][1] < nclistArray[2][1]

        assert nclistArray[2][1] < nclistArray[2][2]
    }

    @IgnoreRest
    void "project a single feature from a chunked scaffold"(){

        given: "proper input"
        String sequenceList = "[{\"name\":\"Group1.10\",\"start\":891079,\"end\":933237,\"reverse\":false,\"feature\":{\"parent_id\":\"Group1.10\",\"name\":\"GB40737-RA\",\"start\":891079,\"end\":933237}}]"
        String refererLoc= "{\"sequenceList\":${sequenceList}}"
        String location = ":2516297..1566327"
        String trackName = "Official Gene Set v3.2"
        String fileName = "lf-2.json"
        String chunkFileName = "test/integration/resources/sequences/honeybee-tracks/tracks/${trackName}/${refererLoc}${location}/${fileName}"
        String trackDataName = "test/integration/resources/sequences/honeybee-tracks/tracks/${trackName}/${refererLoc}${location}/trackData.json"
        JSONArray sequenceArray = new JSONArray(sequenceList)

        when: "we project the data"
        JSONObject trackObject = trackService.projectTrackData(sequenceArray, trackDataName, refererLoc, Organism.first())
        MultiSequenceProjection multiSequenceProjection = projectionService.getCachedProjection(refererLoc)
        def projectionChunkList = multiSequenceProjection.projectionChunkList.projectionChunkList

        then: "should we have multiple chunks (0-2) or map chunk 2 to 0 and get lf-0.json instead"
        assert "Group1.10"==projectionChunkList.get(0).sequence
        assert 3==projectionChunkList.size()


        when: "when we get lf-2.json (or lf-0.json) it should now work"
        JSONArray trackArray = trackService.projectTrackChunk(fileName, chunkFileName, refererLoc, Organism.first(),trackName)

        then: "we expect the start and the stop to be in order and there should be NO overlap"

        assert trackArray.size()>0
    }
}
