
package net.cofcool.toolbox.internal.trello;


import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("unused")
public class Action {

    @JsonProperty("appCreator")
    private AppCreator mAppCreator;
    @JsonProperty("data")
    private Data mData;
    @JsonProperty("date")
    private String mDate;
    @JsonProperty("id")
    private String mId;
    @JsonProperty("idMemberCreator")
    private String mIdMemberCreator;
    @JsonProperty("limits")
    private Limits mLimits;
    @JsonProperty("memberCreator")
    private MemberCreator mMemberCreator;
    @JsonProperty("type")
    private String mType;

    public AppCreator getAppCreator() {
        return mAppCreator;
    }

    public void setAppCreator(AppCreator appCreator) {
        mAppCreator = appCreator;
    }

    public Data getData() {
        return mData;
    }

    public void setData(Data data) {
        mData = data;
    }

    public String getDate() {
        return mDate;
    }

    public void setDate(String date) {
        mDate = date;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getIdMemberCreator() {
        return mIdMemberCreator;
    }

    public void setIdMemberCreator(String idMemberCreator) {
        mIdMemberCreator = idMemberCreator;
    }

    public Limits getLimits() {
        return mLimits;
    }

    public void setLimits(Limits limits) {
        mLimits = limits;
    }

    public MemberCreator getMemberCreator() {
        return mMemberCreator;
    }

    public void setMemberCreator(MemberCreator memberCreator) {
        mMemberCreator = memberCreator;
    }

    public String getType() {
        return mType;
    }

    public void setType(String type) {
        mType = type;
    }

}
