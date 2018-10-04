package org.bbop.apollo.history

/**
 * Created by ndunn on 4/7/15.
 */
enum FeatureOperation {

    ADD_FEATURE,
    DELETE_FEATURE,
    ADD_TRANSCRIPT,
    DELETE_TRANSCRIPT,
    ADD_EXON,
    DELETE_EXON,
    MERGE_EXONS,
    SPLIT_EXON,
    SET_EXON_BOUNDARIES,
    MERGE_TRANSCRIPTS,
    SPLIT_TRANSCRIPT,
    SET_TRANSLATION_START,
    UNSET_TRANSLATION_START,
    SET_TRANSLATION_END,
    UNSET_TRANSLATION_END,
    SET_TRANSLATION_ENDS,
    SET_LONGEST_ORF,
    FLIP_STRAND,
    SET_READTHROUGH_STOP_CODON,
    UNSET_READTHROUGH_STOP_CODON,
    SET_BOUNDARIES,
    CHANGE_ANNOTATION_TYPE,
    ASSOCIATE_TRANSCRIPT_TO_GENE,
    DISSOCIATE_TRANSCRIPT_FROM_GENE

    public String toLower(){
        return name().toLowerCase()
    }
}
