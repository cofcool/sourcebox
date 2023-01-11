
package net.cofcool.toolbox.internal.trello;


import com.google.gson.annotations.SerializedName;


@SuppressWarnings("unused")
public class Action {

    @SerializedName("appCreator")
    private AppCreator mAppCreator;
    @SerializedName("data")
    private Data mData;
    @SerializedName("date")
    private String mDate;
    @SerializedName("id")
    private String mId;
    @SerializedName("idMemberCreator")
    private String mIdMemberCreator;
    @SerializedName("limits")
    private Limits mLimits;
    @SerializedName("memberCreator")
    private MemberCreator mMemberCreator;
    @SerializedName("type")
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
