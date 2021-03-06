package xml_mike.radioalarm.models;

/**
 * Created by MClifford on 18/05/15.
 */
public class RadioStream{

    public long id;
    public long radioId;
    public String title;
    public String description;
    public String slug;
    public String url;
    public boolean status;

    public RadioStream(){
        this.id=0L;
        this.radioId=0L;
        this.title="Not Initialised Correctly";
        this.description="Not Initialised Correctly";
        this.slug="Not Initialised Correctly";
        this.url="Not Initialised Correctly";
        this.status = false;
    } //default constructor, needed for creating lists

    /**
     * Constructer used by factory
     * @param id id of new radio stream, if provided
     * @param radioid radio station that the stream belongs to
     * @param url url of said stream.
     */
    public RadioStream(long id, long radioid,  String url) {
        this.id = id;
        this.radioId = radioid;
        //this.title = title;
        //this.description = description;
        //this.slug = slug;
        this.url = url;
    }
}
