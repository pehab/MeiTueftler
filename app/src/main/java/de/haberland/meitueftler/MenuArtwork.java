package de.haberland.meitueftler;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Picture;
import android.graphics.Rect;
import android.graphics.drawable.PictureDrawable;

/** Resolution-independent little inventions for the two clickable workshop cards. */
final class MenuArtwork extends PictureDrawable {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);

    MenuArtwork(boolean vehicle) {
        super(new Picture());
        Canvas c=getPicture().beginRecording(480,240);
        // A warm workbench, pencil sketches, and screws frame each different invention.
        fill(0xFFF9F0DA);c.drawRoundRect(6,6,474,234,20,20,paint);
        stroke(0xFFE9DABE,2);c.drawRoundRect(6,6,474,234,20,20,paint);
        for(int y=30;y<230;y+=27) {
            stroke(0xFFDCCCAC,1);c.drawLine(16,y,464,y+3,paint);
        }
        bolt(c,23,23);bolt(c,457,23);bolt(c,23,217);bolt(c,457,217);
        if(vehicle)drawVehicle(c);else drawMarbles(c);
        getPicture().endRecording();
    }

    @Override public void draw(Canvas c) {
        Rect bounds=getBounds();if(bounds.isEmpty())return;
        c.save();c.translate(bounds.left,bounds.top);
        float scale=Math.min(bounds.width()/480f,bounds.height()/240f);
        c.translate((bounds.width()-480*scale)/2,(bounds.height()-240*scale)/2);c.scale(scale,scale);
        c.drawPicture(getPicture());
        c.restore();
    }

    private void drawMarbles(Canvas c) {
        // The visible route goes from a high wooden ramp, via a spring, into a basket.
        stroke(0xFFCEBDA0,2);paint.setStrokeCap(Paint.Cap.ROUND);
        c.drawLine(56,176,88,198,paint);c.drawLine(88,176,56,198,paint);
        c.drawCircle(418,66,19,paint);c.drawLine(418,38,418,44,paint);
        c.drawLine(441,66,449,66,paint);c.drawLine(396,66,388,66,paint);
        plank(c,58,94,240,136,18);plank(c,223,159,340,113,17);
        // Dotted flight path and small speed marks give the static picture movement.
        fill(0xFF7BBEB0);
        for(int i=0;i<8;i++) {
            float t=i/7f;c.drawCircle(281+113*t,99-100*t+82*t*t,3,paint);
        }
        ball(c,105,92,16,MainActivity.TEAL);ball(c,315,75,17,0xFFE0A139);
        stroke(0xFF7BBEB0,3);c.drawLine(79,75,90,78,paint);c.drawLine(78,85,86,87,paint);
        // Spring trampoline under the low end of the first ramp.
        stroke(MainActivity.INK,4);Path spring=new Path();spring.moveTo(236,195);
        for(int i=0;i<7;i++)spring.lineTo(i%2==0?225:246,190-i*5);
        spring.lineTo(236,151);c.drawPath(spring,paint);
        fill(MainActivity.ORANGE);c.drawRoundRect(211,143,260,153,5,5,paint);
        fill(0xFFB79160);c.drawRoundRect(211,196,260,202,3,3,paint);
        // Teal basket is clearly different from the wheeled machine on the other card.
        Path basket=new Path();basket.moveTo(373,132);basket.lineTo(431,132);
        basket.lineTo(422,182);basket.lineTo(382,182);basket.close();
        fill(0xFF73A799);c.drawPath(basket,paint);stroke(MainActivity.INK,3);c.drawPath(basket,paint);
        for(int x=385;x<429;x+=11)c.drawLine(x,137,402+(x-402)*0.65f,178,paint);
        c.drawLine(379,150,427,150,paint);c.drawLine(381,166,424,166,paint);
        fill(0xFFEBC783);c.drawRoundRect(367,126,437,137,5,5,paint);
        bolt(c,378,132);bolt(c,427,132);
        ball(c,402,118,13,MainActivity.ORANGE);
        // A tiny pencil invites further experimenting.
        pencil(c,80,210,-8);
    }

    private void drawVehicle(Canvas c) {
        stroke(0xFFCEBDA0,2);c.drawCircle(375,48,21,paint);
        c.drawCircle(375,48,8,paint);
        for(int i=0;i<8;i++) {
            double a=i*Math.PI/4;c.drawLine(375+(float)Math.cos(a)*25,48+(float)Math.sin(a)*25,
                    375+(float)Math.cos(a)*30,48+(float)Math.sin(a)*30,paint);
        }
        // The machine climbs a small ramp rather than floating against a plain backdrop.
        Path hill=new Path();hill.moveTo(51,205);hill.lineTo(260,205);
        hill.lineTo(427,155);hill.lineTo(442,205);hill.close();
        fill(0xFFE0C198);c.drawPath(hill,paint);stroke(0xFFB79160,4);
        c.drawLine(260,205,427,155,paint);c.drawLine(51,205,260,205,paint);
        c.save();c.rotate(-10,240,149);
        plank(c,116,144,352,144,25);plank(c,170,132,204,77,16);
        plank(c,204,77,282,132,16);plank(c,170,132,282,132,16);
        wheel(c,139,174,32);wheel(c,329,174,32);
        // Same teal motor and M as in the actual vehicle builder.
        fill(MainActivity.INK);c.drawRoundRect(208,97,268,147,9,9,paint);
        fill(MainActivity.TEAL);c.drawRoundRect(212,93,264,142,9,9,paint);
        fill(0xFFBED9CC);c.drawRoundRect(217,98,259,109,4,4,paint);
        fill(0xFFFFFFFF);paint.setTextSize(23);paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        c.drawText("M",228,134,paint);paint.setTypeface(android.graphics.Typeface.DEFAULT);
        bolt(c,219,135);bolt(c,258,135);
        // Little golden drive gear and exposed axle make it look like a child's invention.
        fill(0xFFE4B54E);c.drawCircle(287,146,15,paint);
        stroke(0xFFA67F35,3);
        for(int i=0;i<8;i++){c.save();c.rotate(i*45,287,146);c.drawLine(287,126,287,132,paint);c.restore();}
        bolt(c,287,146);c.restore();
        stroke(MainActivity.ORANGE,3);c.drawLine(77,122,99,122,paint);
        c.drawLine(66,139,91,139,paint);c.drawLine(75,157,97,157,paint);
        // Destination flag and hand-drawn motion arrow.
        stroke(MainActivity.INK,3);c.drawLine(421,93,421,151,paint);
        Path flag=new Path();flag.moveTo(423,93);flag.lineTo(449,101);flag.lineTo(423,112);flag.close();
        fill(MainActivity.ORANGE);c.drawPath(flag,paint);
        stroke(0xFFC59562,2);c.drawLine(68,53,142,53,paint);
        c.drawLine(142,53,132,46,paint);c.drawLine(142,53,132,60,paint);
        pencil(c,66,216,3);
    }

    private void plank(Canvas c,float x1,float y1,float x2,float y2,float thickness) {
        c.save();c.translate(x1,y1);c.rotate((float)Math.toDegrees(Math.atan2(y2-y1,x2-x1)));
        float length=(float)Math.hypot(x2-x1,y2-y1);
        fill(0xFFC19360);c.drawRoundRect(-7,-thickness/2,length+7,thickness/2,5,5,paint);
        fill(0xFFE8BC81);c.drawRoundRect(-7,-thickness/2,length+7,thickness/2-4,5,5,paint);
        stroke(0xFFCA9C67,1.4f);c.drawLine(16,-3,length-13,-3,paint);
        c.drawLine(24,3,length-21,3,paint);c.drawOval(length*0.5f-9,-4,length*0.5f+9,3,paint);
        bolt(c,1,0);bolt(c,length-1,0);c.restore();
    }
    private void wheel(Canvas c,float x,float y,float r) {
        fill(MainActivity.INK);c.drawCircle(x,y,r,paint);
        stroke(0xFF567C70,3);c.drawCircle(x,y,r-5,paint);
        fill(0xFFEBC783);c.drawCircle(x,y,r-12,paint);
        stroke(0xFFA67F35,3);
        for(int i=0;i<6;i++){c.save();c.rotate(i*60,x,y);c.drawLine(x,y-7,x,y-r+14,paint);c.restore();}
        bolt(c,x,y);
    }
    private void ball(Canvas c,float x,float y,float radius,int color) {
        fill(0x22000000);c.drawOval(x-radius,y+radius-3,x+radius,y+radius+4,paint);
        fill(color);c.drawCircle(x,y,radius,paint);
        fill(0x99FFFFFF);c.drawCircle(x-radius*0.3f,y-radius*0.35f,radius*0.3f,paint);
    }
    private void bolt(Canvas c,float x,float y) {
        fill(0xFFB1B5A5);c.drawCircle(x,y,3.5f,paint);
        stroke(0xFF647773,1.3f);c.drawLine(x-2,y+1.5f,x+2,y-1.5f,paint);
    }
    private void pencil(Canvas c,float x,float y,float rotation) {
        c.save();c.rotate(rotation,x,y);fill(0xFFE0A139);c.drawRoundRect(x,y-3,x+63,y+3,2,2,paint);
        fill(MainActivity.ORANGE);c.drawRect(x,y-3,x+8,y+3,paint);
        Path tip=new Path();tip.moveTo(x+63,y-3);tip.lineTo(x+73,y);tip.lineTo(x+63,y+3);tip.close();
        fill(MainActivity.INK);c.drawPath(tip,paint);c.restore();
    }
    private void fill(int color) { paint.setStyle(Paint.Style.FILL);paint.setColor(color); }
    private void stroke(int color,float width) { paint.setStyle(Paint.Style.STROKE);paint.setColor(color);paint.setStrokeWidth(width); }
}
