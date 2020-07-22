package moka.net.a4;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;

public class Team implements Serializable {
    private String city;
    private String name;
    private String sport;
    private String mvp;
    private byte[] image;

    public Team(){
       this.city="";
       this.name="";
       this.sport="";
       this.mvp="";
    }

    public Team(String city, String name, String sport, String mvp){
        this.city=city;
        this.name=name;
        this.sport=sport;
        this.mvp=mvp;
    }

    public Team(String city, String name, String sport, String mvp, Bitmap imageRes){
        this.city=city;
        this.name=name;
        this.sport=sport;
        this.mvp=mvp;
        this.image = fromImgToBytes(imageRes);
    }

    public String getCity() {
        return city;
    }

    public String getMvp() {
        return mvp;
    }

    public String getName() {
        return name;
    }

    public String getSport() {
        if (sport=="") return "0";
        else return sport;
    }

    public Bitmap getImage() {
        return fromBytesToImg(this.image);
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setMvp(String mvp) {
        this.mvp = mvp;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSport(String sport) {
        this.sport = sport;
    }

    public void setImage(Bitmap img) {
        this.image = fromImgToBytes(img);
    }

    public byte[] getImgBytes(){
        return image;
    }

    public byte[] fromImgToBytes(Bitmap pic) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        pic.compress(Bitmap.CompressFormat.PNG, 0, stream);
        return stream.toByteArray();
    }

    public Bitmap fromBytesToImg(byte[] pic) {
        return BitmapFactory.decodeByteArray(pic, 0, pic.length);
    }

}
