package org.bbop.apollo

class DBXref {

    static constraints = {
    }

//    Integer dbxrefId;
    String version;
    DB db;
    String accession;
    String description;
//    Set<DBXrefProperty> dbxrefProperties = new HashSet<DBXrefProperty>(0);

    static hasMany = [
            dbxrefProperties : DBXrefProperty
    ]


    public boolean equals(Object other) {
        if ( (this == other ) ) return true;
        if ( (other == null ) ) return false;
        if ( !(other instanceof DBXref) ) return false;
        DBXref castOther = ( DBXref ) other;

        return ( (this.getVersion()==castOther.getVersion()) || ( this.getVersion()!=null && castOther.getVersion()!=null && this.getVersion().equals(castOther.getVersion()) ) ) && ( (this.getDb()==castOther.getDb()) || ( this.getDb()!=null && castOther.getDb()!=null && this.getDb().equals(castOther.getDb()) ) ) && ( (this.getAccession()==castOther.getAccession()) || ( this.getAccession()!=null && castOther.getAccession()!=null && this.getAccession().equals(castOther.getAccession()) ) );
    }

    public int hashCode() {
        int result = 17;


        result = 37 * result + ( getVersion() == null ? 0 : this.getVersion().hashCode() );
        result = 37 * result + ( getDb() == null ? 0 : this.getDb().hashCode() );
        result = 37 * result + ( getAccession() == null ? 0 : this.getAccession().hashCode() );


        return result;
    }

    public DBXref generateClone() {
        DBXref cloned = new DBXref();
        cloned.db = this.db;
        cloned.accession = this.accession;
        cloned.description = this.description;
        cloned.dbxrefProperties = this.dbxrefProperties;
        return cloned;
    }

}
