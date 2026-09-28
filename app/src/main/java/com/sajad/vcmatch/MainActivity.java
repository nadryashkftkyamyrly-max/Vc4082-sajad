package com.sajad.vcmatch;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.Locale;

public class MainActivity extends Activity {
    EditText frequency,inA,inB,ns,targetR,vPrimary,ae,bmax,al,lCoil,rCoil,rSoil;
    Spinner inputMode;
    TextView result;
    final String[] modes={"Rs / Xs از VC4082","|Z| / θ از VC4082"};

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        try { buildUi(); }
        catch(Throwable e){
            TextView t=new TextView(this); t.setPadding(24,24,24,24); t.setTextSize(16); t.setText("خطای شروع برنامه:\n"+e.toString()); setContentView(t);
        }
    }

    private TextView label(String s,int size){ TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(Color.rgb(25,60,90)); t.setPadding(4,14,4,6); return t; }
    private EditText field(String hint,String value){ EditText e=new EditText(this); e.setHint(hint); e.setText(value); e.setTextSize(16); e.setSingleLine(true); e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED); e.setLayoutParams(new LinearLayout.LayoutParams(-1,-2)); return e; }
    private Button button(String text){ Button b=new Button(this); b.setText(text); b.setLayoutParams(new LinearLayout.LayoutParams(-1,-2)); return b; }

    private void buildUi(){
        ScrollView scroll=new ScrollView(this);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(24,24,24,36); root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scroll.addView(root,new ScrollView.LayoutParams(-1,-2));

        TextView title=label("محاسبه‌گر VC4082، مچینگ و ترانس",23); title.setGravity(Gravity.CENTER); root.addView(title);
        TextView sub=label("مچینگ قبل و بعد ترانس + محاسبات ترانس",14); sub.setGravity(Gravity.CENTER); root.addView(sub);

        root.addView(label("۱) ورودی VC4082",18));
        frequency=field("فرکانس Hz","5000"); root.addView(frequency);
        inputMode=new Spinner(this); inputMode.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,modes)); root.addView(inputMode,new LinearLayout.LayoutParams(-1,-2));
        inA=field("Rs (Ω)",""); root.addView(inA); inB=field("Xs (Ω؛ با علامت)",""); root.addView(inB);
        inputMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){ public void onItemSelected(AdapterView<?> p,View v,int pos,long id){ if(pos==0){inA.setHint("Rs (Ω)");inB.setHint("Xs (Ω؛ با علامت)");}else{inA.setHint("|Z| (Ω)");inB.setHint("θ (درجه؛ با علامت)");}} public void onNothingSelected(AdapterView<?> p){} });

        root.addView(label("۲) ترانس و هدف آمپ",18));
        ns=field("تعداد دور ثانویه Ns","1200"); root.addView(ns);
        targetR=field("بار هدف آمپ (Ω)","4"); root.addView(targetR);
        vPrimary=field("ولتاژ سینوسی اولیه Vrms","14"); root.addView(vPrimary);
        ae=field("Ae هسته (mm²)","368"); root.addView(ae);
        bmax=field("Bmax (T)","0.15"); root.addView(bmax);
        al=field("AL (nH/turn²) اختیاری","5470"); root.addView(al);

        root.addView(label("۳) اختیاری: کویل و خاک",18));
        lCoil=field("L کویل (mH)",""); root.addView(lCoil);
        rCoil=field("Rs کویل (Ω)",""); root.addView(rCoil);
        rSoil=field("Rs خاک (Ω)",""); root.addView(rSoil);

        Button calc=button("محاسبه"); root.addView(calc); calc.setOnClickListener(v->calculate());
        Button clear=button("پاک کردن ورودی اندازه‌گیری"); root.addView(clear); clear.setOnClickListener(v->{inA.setText("");inB.setText("");result.setText("");});
        result=label("نتایج اینجا نمایش داده می‌شود.",15); result.setTextIsSelectable(true); result.setPadding(12,20,12,30); root.addView(result,new LinearLayout.LayoutParams(-1,-2));
        setContentView(scroll);
    }

    private double d(EditText e)throws Exception{String s=e.getText().toString().trim().replace(',','.');if(s.isEmpty())throw new Exception("یکی از ورودی‌های ضروری خالی است");return Double.parseDouble(s);}
    private Double opt(EditText e){try{String s=e.getText().toString().trim().replace(',','.');return s.isEmpty()?null:Double.parseDouble(s);}catch(Exception x){return null;}}
    private String n(double v,String unit){double a=Math.abs(v);if(a>=1e6)return String.format(Locale.US,"%.3f M%s",v/1e6,unit);if(a>=1e3)return String.format(Locale.US,"%.3f k%s",v/1e3,unit);if(a<1e-6&&a>0)return String.format(Locale.US,"%.3f n%s",v*1e9,unit);if(a<1e-3&&a>0)return String.format(Locale.US,"%.3f µ%s",v*1e6,unit);return String.format(Locale.US,"%.3f %s",v,unit);}
    private String cap(double c){if(c>=1e-6)return String.format(Locale.US,"%.3f µF",c*1e6);if(c>=1e-9)return String.format(Locale.US,"%.3f nF",c*1e9);return String.format(Locale.US,"%.3f pF",c*1e12);}
    private String ind(double l){if(l>=1)return String.format(Locale.US,"%.3f H",l);if(l>=1e-3)return String.format(Locale.US,"%.3f mH",l*1e3);return String.format(Locale.US,"%.3f µH",l*1e6);}
    private String reactiveMatch(double x,double f){if(Math.abs(x)<0.01)return "نیاز به جبران راکتیو محسوس نیست.";if(x>0)return "خازن سری ≈ "+cap(1.0/(2*Math.PI*f*x));return "سلف سری ≈ "+ind(Math.abs(x)/(2*Math.PI*f));}

    private void calculate(){
        try{
            double f=d(frequency),a=d(inA),b=d(inB),Ns=d(ns),Rt=d(targetR),Vp=d(vPrimary),Ae=d(ae)*1e-6,B=d(bmax);
            if(f<=0||Ns<=0||Rt<=0||Vp<0||Ae<=0||B<=0)throw new Exception("ورودی‌ها باید معتبر و مثبت باشند");
            double Rs,Xs,Z,theta;
            if(inputMode.getSelectedItemPosition()==0){Rs=a;Xs=b;Z=Math.hypot(Rs,Xs);theta=Math.toDegrees(Math.atan2(Xs,Rs));}
            else{Z=a;theta=b;double r=Math.toRadians(theta);Rs=Z*Math.cos(r);Xs=Z*Math.sin(r);}
            if(Rs<=0)throw new Exception("Rs باید بزرگ‌تر از صفر باشد");
            double ratio=Math.sqrt(Rs/Rt),NpMatch=Ns/ratio,Nmin=Vp/(4.44*f*Ae*B),Np=Math.ceil(Math.max(NpMatch,Nmin));
            double actualRatio=Ns/Np,Rpri=Rs/(actualRatio*actualRatio),Xpri=Xs/(actualRatio*actualRatio),Zpri=Math.hypot(Rpri,Xpri),thPri=Math.toDegrees(Math.atan2(Xpri,Rpri));
            double Vs=Vp*actualRatio,flux=Vp/(4.44*f*Np*Ae);
            StringBuilder s=new StringBuilder();
            s.append("بار اندازه‌گیری‌شده\nRs = ").append(n(Rs,"Ω")).append("\nXs = ").append(n(Xs,"Ω")).append("\n|Z| = ").append(n(Z,"Ω")).append("\nθ = ").append(String.format(Locale.US,"%.2f°",theta)).append("\n\n");
            s.append("مچینگ بعد ترانس\n").append(reactiveMatch(Xs,f)).append("\n\n");
            s.append("ترانس\nنسبت ایده‌آل Ns/Np = ").append(String.format(Locale.US,"%.3f",ratio)).append("\nNp تطبیق = ").append(String.format(Locale.US,"%.1f دور",NpMatch)).append("\nNp حداقل شار = ").append(String.format(Locale.US,"%.1f دور",Nmin)).append("\nNp پیشنهادی = ").append(String.format(Locale.US,"%.0f دور",Np)).append("\nنسبت واقعی = 1:").append(String.format(Locale.US,"%.3f",actualRatio)).append("\nVs ایده‌آل = ").append(String.format(Locale.US,"%.1f Vrms",Vs)).append("\nBpeak = ").append(String.format(Locale.US,"%.3f T",flux)).append("\n\n");
            s.append("بازتاب روی اولیه\nZpri ≈ ").append(String.format(Locale.US,"%.3f + j%.3f Ω",Rpri,Xpri)).append("\n|Zpri| = ").append(String.format(Locale.US,"%.3f Ω",Zpri)).append(" ، θ = ").append(String.format(Locale.US,"%.2f°",thPri)).append("\n\nمچینگ قبل ترانس\n").append(reactiveMatch(Xpri,f)).append("\n");
            Double AL=opt(al);if(AL!=null&&AL>0){double Lm=AL*1e-9*Np*Np;s.append("\nLm اولیه ≈ ").append(ind(Lm)).append("\nXm ≈ ").append(n(2*Math.PI*f*Lm,"Ω")).append("\n");}
            result.setText(s.toString());
        }catch(Throwable e){result.setText("خطا: "+e.getMessage());}
    }
}
