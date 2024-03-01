
package net.cofcool.sourcebox.internal.trello;


import com.fasterxml.jackson.annotation.JsonProperty;


@SuppressWarnings("unused")
public class Label {

    @JsonProperty("color")
    private String mColor;
    @JsonProperty("id")
    private String mId;
    @JsonProperty("idBoard")
    private String mIdBoard;
    @JsonProperty("name")
    private String mName;

    public String getColor() {
        return mColor;
    }

    public void setColor(String color) {
        mColor = color;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getIdBoard() {
        return mIdBoard;
    }

    public void setIdBoard(String idBoard) {
        mIdBoard = idBoard;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

}
