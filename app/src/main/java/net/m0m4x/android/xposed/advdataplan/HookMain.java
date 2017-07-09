package net.m0m4x.android.xposed.advdataplan;
/**
 * Created by max on 09/04/2017.
 */

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.XModuleResources;
import android.os.Build;
import android.text.format.Time;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static java.lang.Math.abs;


public class HookMain implements IXposedHookZygoteInit, IXposedHookLoadPackage, IXposedHookInitPackageResources {


    /****************************
        RESOURCES Hooking

     */

    private static String MODULE_PATH = null;

    int R_layout_data_usage_cycle_editor;
    int R_id_datepicker;
    int R_id_cycle_days;
    int R_id_cycle_day;

    int modR_strings_dataplan_days;
    int modR_strings_dataplan_day;
    int modR_strings_nr1_daily;
    int modR_strings_nr7_weekly;
    int modR_strings_nr30_fixedmonth;
    int modR_strings_nr31_monthly;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        MODULE_PATH = startupParam.modulePath;
        //XposedBridge.log("HOOK initZygote - " + startupParam.modulePath + " !");
    }

    @Override
    public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
        if (!resparam.packageName.equals("com.android.settings")) {
            return;
        }
        XposedBridge.log("HOOK RES init -  " + resparam.packageName + " !");

        /*
        Get ID of module resources
         */
        XModuleResources modRes = XModuleResources.createInstance(MODULE_PATH, resparam.res);
        modR_strings_dataplan_days = resparam.res.addResource(modRes, R.string.dataplan_days);
        modR_strings_dataplan_day = resparam.res.addResource(modRes, R.string.dataplan_day);
        modR_strings_nr1_daily = resparam.res.addResource(modRes, R.string.nr1_daily);
        modR_strings_nr7_weekly = resparam.res.addResource(modRes, R.string.nr7_weekly);
        modR_strings_nr30_fixedmonth = resparam.res.addResource(modRes, R.string.nr30_fixedmonth);
        modR_strings_nr31_monthly = resparam.res.addResource(modRes, R.string.nr31_monthly);

        /*
        Get ID of native resources
         */
        R_layout_data_usage_cycle_editor = resparam.res.getIdentifier("data_usage_cycle_editor", "layout", "com.android.settings");
        XposedBridge.log("HOOK RES       ...found R.layout.data_usage_cycle_editor : " + R_layout_data_usage_cycle_editor + " !");

        /*
        Replace Resource
         */
        /*
        XModuleResources modRes = XModuleResources.createInstance(MODULE_PATH, resparam.res);
        resparam.res.setReplacement("com.android.settings", "layout", "data_usage_cycle_editor", modRes.fwd(R.layout.data_usage_adv_cycle_editor));
        XposedBridge.log("HOOK RES replaced!");
        */

        /*
        Hook Layout
        */
        resparam.res.hookLayout("com.android.settings", "layout", "data_usage_cycle_editor", new XC_LayoutInflated() {
            @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
            @Override
            public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {

                XposedBridge.log("HOOK RES layout is inflating... - data_usage_cycle_editor!");

                Context context = liparam.view.getContext();

                /*
                layout 0 : [
                            txtview     <= original    (visibility GONE)
                            numpicker   <= original    (visibility GONE)
                            + layout 1     [    datepicker    ]
                            + layout 2     [txtview  numpicker]
                           ]
                 */
                int i150dip = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150, context.getResources().getDisplayMetrics());
                int i100dip = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, context.getResources().getDisplayMetrics());
                int i48dip = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
                int i16dip = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, context.getResources().getDisplayMetrics());

                //layout 0
                R_id_cycle_day = liparam.res.getIdentifier("cycle_day", "id", "com.android.settings");
                NumberPicker l0_num =  (NumberPicker) liparam.view.findViewById(R_id_cycle_day);
                LinearLayout res_layout0 = (LinearLayout) l0_num.getRootView();
                res_layout0.setOrientation(LinearLayout.VERTICAL);

                //hide all existing view
                for (int i=0; i<res_layout0.getChildCount();i++){
                    res_layout0.getChildAt(i).setVisibility(View.GONE);
                }

                //layout 1
                LinearLayout res_layout1 = new LinearLayout(context);
                res_layout1.setOrientation(LinearLayout.HORIZONTAL);
                TextView l1_txt = new TextView(context);
                LinearLayout.LayoutParams l1_txt_lp = new LinearLayout.LayoutParams( LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
                l1_txt_lp.gravity = Gravity.CENTER_VERTICAL;
                l1_txt_lp.setMarginStart(i16dip);
                l1_txt.setLayoutParams(l1_txt_lp);
                l1_txt.setGravity(Gravity.CENTER_VERTICAL);
                //l2_txt.setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault_Medium);
                l1_txt.setText(modR_strings_dataplan_day);
                DatePickerDialog l1_dat = new DatePickerDialog(context, android.R.style.Theme_Holo_Light_Dialog, null, 2017,04,16);
                l1_dat.getDatePicker().findViewById(context.getResources().getIdentifier("year","id","android")).setVisibility(View.GONE);
                l1_dat.getDatePicker().setId(View.generateViewId());
                R_id_datepicker = l1_dat.getDatePicker().getId();
                l1_dat.getDatePicker().setLayoutParams(new LinearLayout.LayoutParams( LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
                l1_dat.getDatePicker().setCalendarViewShown(false);
                l1_dat.getDatePicker().setSpinnersShown(true);
                //l1_dat.getDatePicker().setVisibility(View.GONE);
                res_layout1.addView(l1_txt);
                res_layout1.addView(l1_dat.getDatePicker());

                //layout 2
                LinearLayout res_layout2 = new LinearLayout(context);
                res_layout2.setOrientation(LinearLayout.HORIZONTAL);
                TextView l2_txt = new TextView(context);
                LinearLayout.LayoutParams l2_txt_lp = new LinearLayout.LayoutParams( LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
                l2_txt_lp.gravity = Gravity.CENTER_VERTICAL;
                l2_txt_lp.setMarginStart(i16dip);
                l2_txt.setLayoutParams(l2_txt_lp);
                l2_txt.setGravity(Gravity.CENTER_VERTICAL);
                //l2_txt.setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault_Medium);
                l2_txt.setText(modR_strings_dataplan_days);
                NumberPicker l2_num = new NumberPicker(context);
                l2_num.setId(View.generateViewId());
                R_id_cycle_days = l2_num.getId();
                LinearLayout.LayoutParams l2_num_lp = new LinearLayout.LayoutParams( i150dip, i100dip);
                l2_num_lp.setMarginEnd(i16dip);
                l2_num_lp.setMarginStart(i16dip);
                l2_num.setLayoutParams(l2_num_lp);
                l2_num.setGravity(Gravity.CENTER_VERTICAL);
                res_layout2.addView(l2_txt);
                res_layout2.addView(l2_num);

                //Adding Layouts
                res_layout0.addView(res_layout1);
                res_layout0.addView(res_layout2);

                XposedBridge.log("HOOK RES layout is inflated!");
            }
        });

    }





    /****************************
        METHODS Hooking

     */

    public static final int CYCLE_NONE = -1;
    private static final String EXTRA_TEMPLATE = "template";

    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {

        XposedBridge.log("HOOK handleLoadPackage - " + lpparam.packageName + " !");

        if(         lpparam.packageName.equals("com.android.settings")
                ||  lpparam.packageName.equals("android")) {
            XposedBridge.log("HOOK NetworkPolicyManager methods! (pkg:"+lpparam.packageName+")");

            final Class<?> NetworkPolicyManager = XposedHelpers.findClass(
                    "android.net.NetworkPolicyManager",
                    lpparam.classLoader);
            final Class<?> NetworkPolicy = XposedHelpers.findClass(
                    "android.net.NetworkPolicy",
                    lpparam.classLoader);

            findAndHookMethod(NetworkPolicyManager, "computeNextCycleBoundary", long.class , NetworkPolicy , new XC_MethodReplacement() {

                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("HOOK android.net > computeNextCycleBoundary !!! (pkg:"+lpparam.packageName+")");

                    // Get Params
                    long currentTime = (long) param.args[0];    // long currentTime
                    Object policy = param.args[1];              // NetworkPolicy policy
                    int cycle_day = (int) XposedHelpers.getObjectField(policy, "cycleDay");

                    // Check
                    if (cycle_day == CYCLE_NONE) {
                        throw new IllegalArgumentException("Unable to compute boundary without cycleDay");
                    }

                    // Get Preferences
                    Calendar pref_cycleDate = Calendar.getInstance();
                    int pref_cycleDays = 31;
                    // test cycle_day of NetworkPolicy
                    if(cycle_day < 31){
                        //Not Bitshifted
                        pref_cycleDate.set(Calendar.DAY_OF_MONTH, cycle_day);
                    } else {
                        //Decode Bitshifted Int :D
                        try{
                            int bs1 = cycle_day & 0xFF;          // Day of Month
                            int bs2 = (cycle_day >> 8) & 0xFF;   // Month
                            int bs3 = (cycle_day >> 16) & 0xFF;  // num Days
                            XposedBridge.log("HOOK REQ loaded bitshited Ints "+bs1+"."+bs2+"."+bs3+"");
                            pref_cycleDate.set(Calendar.DAY_OF_MONTH, bs1);
                            pref_cycleDate.set(Calendar.MONTH, bs2);
                            if(pref_cycleDate.getTimeInMillis() > System.currentTimeMillis()) {
                                pref_cycleDate.set(Calendar.YEAR, pref_cycleDate.get(Calendar.YEAR) - 1);
                                XposedBridge.log("HOOK REQ preference year set to "+pref_cycleDate.get(Calendar.YEAR)+"");
                            }
                            pref_cycleDays = bs3;
                        } catch (ClassCastException e){
                            XposedBridge.log("HOOK REQ Error decoding bitshifted Ints :"+ e.toString() );
                        }
                    }

                    //Debug - Wait... What is the request?
                    Format format = new SimpleDateFormat("dd/MM/yyyy");
                    XposedBridge.log("HOOK REQ NEXT Cycle with prefTime:"+format.format(new Date(pref_cycleDate.getTimeInMillis()))+
                            "*"+pref_cycleDays+"  for currentTime:"+format.format(new Date(currentTime)));

                    // Approach to date currentTime, when i am close choose Last <- or Next ->
                    Calendar cycleDate = (Calendar) pref_cycleDate.clone();
                    int m; if (cycleDate.getTimeInMillis()>currentTime) m = -1; else m = 1;
                    while ( daysBetween(cycleDate.getTimeInMillis(), currentTime) >= pref_cycleDays ) {
                        XposedBridge.log("HOOK               " + m + " " + pref_cycleDays );
                        cycleDate.add(Calendar.DAY_OF_YEAR, pref_cycleDays * m);
                    }
                    // Set Next Cycle
                    if(cycleDate.getTimeInMillis()<=currentTime) cycleDate.add(Calendar.DAY_OF_YEAR, pref_cycleDays);

                    //Debug - Ok... That's my result.
                    XposedBridge.log("HOOK       from prefTime:"+format.format(new Date(pref_cycleDate.getTimeInMillis()))+
                            " NEXT to currentTime:"+format.format(new Date(currentTime))+
                            " is "+format.format(new Date(cycleDate.getTimeInMillis())) );

                    //Return Last
                    return cycleDate.getTimeInMillis();

                }

            });

            findAndHookMethod(NetworkPolicyManager, "computeLastCycleBoundary", long.class , NetworkPolicy , new XC_MethodReplacement() {

                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("HOOK  android.net > computeLastCycleBoundary (pkg:"+lpparam.packageName+")");

                    // Get Params
                    long currentTime = (long) param.args[0];    // long currentTime
                    Object policy = param.args[1];              // NetworkPolicy policy
                    int cycle_day = (int) XposedHelpers.getObjectField(policy, "cycleDay");

                    //TODO Check policy template mobile, otherwise do default

                    // Check
                    if (cycle_day == CYCLE_NONE) {
                        throw new IllegalArgumentException("Unable to compute boundary without cycleDay");
                    }

                    // Get Preferences
                    Calendar pref_cycleDate = Calendar.getInstance();
                    int pref_cycleDays = 31;
                    // test cycle_day of NetworkPolicy
                    if(cycle_day < 31){
                        //Not Bitshifted
                        pref_cycleDate.set(Calendar.DAY_OF_MONTH, cycle_day);
                    } else {
                        //Decode Bitshifted Int :D
                        try{
                            int bs1 = cycle_day & 0xFF;          // Day of Month
                            int bs2 = (cycle_day >> 8) & 0xFF;   // Month
                            int bs3 = (cycle_day >> 16) & 0xFF;  // num Days
                            XposedBridge.log("HOOK REQ loaded bitshited Ints "+bs1+"."+bs2+"."+bs3+"");
                            pref_cycleDate.set(Calendar.DAY_OF_MONTH, bs1);
                            pref_cycleDate.set(Calendar.MONTH, bs2);
                            if(pref_cycleDate.getTimeInMillis() > System.currentTimeMillis()) {
                                pref_cycleDate.set(Calendar.YEAR, pref_cycleDate.get(Calendar.YEAR) - 1);
                                XposedBridge.log("HOOK preference year set to "+pref_cycleDate.get(Calendar.YEAR)+"");
                            }
                            pref_cycleDays = bs3;
                        } catch (ClassCastException e){
                            XposedBridge.log("HOOK REQ Error decoding bitshifted Ints :"+ e.toString() );
                        }
                    }

                    //Debug - Wait... What is the request?
                    Format format = new SimpleDateFormat("dd/MM/yyyy");
                    XposedBridge.log("HOOK REQ LAST Cycle with prefTime:"+format.format(new Date(pref_cycleDate.getTimeInMillis()))+
                                                "*"+pref_cycleDays+"  for currentTime:"+format.format(new Date(currentTime)));

                    // Approach to date currentTime, when i am close choose Last <- or Next ->
                    Calendar cycleDate = (Calendar) pref_cycleDate.clone();
                    int m; if (cycleDate.getTimeInMillis()>=currentTime) m = -1; else m = 1;
                    while ( daysBetween(cycleDate.getTimeInMillis(), currentTime) >= pref_cycleDays ) {
                        cycleDate.add(Calendar.DAY_OF_YEAR, pref_cycleDays * m);
                    }
                    // Set Last Cycle
                    if(cycleDate.getTimeInMillis()>=currentTime) cycleDate.add(Calendar.DAY_OF_YEAR, -pref_cycleDays);

                    //Debug - Ok... That's my result.
                    XposedBridge.log("HOOK       from prefTime:"+format.format(new Date(pref_cycleDate.getTimeInMillis()))+
                            " LAST to currentTime:"+format.format(new Date(currentTime))+
                            " is "+format.format(new Date(cycleDate.getTimeInMillis())) );

                    //Return Last
                    return cycleDate.getTimeInMillis();

                }

            });

        }

        if(         lpparam.packageName.equals("com.android.settings")
                ||  lpparam.packageName.equals("android")) {
            XposedBridge.log("HOOK DataUsageSummary methods! (pkg:"+lpparam.packageName+")!");

            /*
            Hook Method
            */
            findAndHookMethod("com.android.settings.DataUsageSummary.CycleEditorFragment", lpparam.classLoader, "onCreateDialog", "android.os.Bundle" , new XC_MethodReplacement() {

                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {

                    XposedBridge.log("HOOK onCreateDialog START!");

                    //final Context context = getActivity();
                    DialogFragment mCycleEditorFragment = (DialogFragment) param.thisObject;                    //CycleEditorFragment
                    //View viewDialog = (View) XposedHelpers.callMethod(mCycleEditorFragment,"getView");        //getView();          is NULL
                    //final Context context = mCycleEditorFragment.getContext();                                //SubSettings
                    final Context context = (Context) XposedHelpers.callMethod(param.thisObject,"getActivity");

                    //final DataUsageSummary target = (DataUsageSummary) getTargetFragment();
                    //final NetworkPolicyEditor editor = target.mPolicyEditor;
                    final Fragment target = mCycleEditorFragment.getTargetFragment();
                    final Object editor = XposedHelpers.getObjectField(target, "mPolicyEditor");                //type NetworkPolicyEditor

                    final AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    final LayoutInflater dialogInflater = LayoutInflater.from(builder.getContext());

                    final View view = (View) XposedHelpers.callMethod(dialogInflater, "inflate", R_layout_data_usage_cycle_editor, null, false);

                    final Object template = mCycleEditorFragment.getArguments().getParcelable(EXTRA_TEMPLATE);      //type NetworkTemplate
                    final int cycleDay = (int) XposedHelpers.callMethod(editor,"getPolicyCycleDay", template);

                    builder.setTitle("Advanced Cycle Editor");  //R.string.data_usage_cycle_editor_title
                    builder.setView(view);

                    /*
                    XposedBridge.log("HOOK R_id_cycle_day="+R_id_cycle_day);
                    XposedBridge.log("HOOK R_id_cycle_days="+R_id_cycle_days);
                    XposedBridge.log("HOOK R_id_datepicker="+R_id_datepicker);
                    view_dump(view);
                    */

                    // Get Preferences
                    Calendar pref_cycle_date = Calendar.getInstance();
                    int pref_cycle_days = 31;

                    XposedBridge.log("HOOK pref LOAD "+cycleDay+"");
                    if(cycleDay < 31){
                        pref_cycle_date.set(Calendar.DAY_OF_MONTH, cycleDay);
                    } else {
                        try{
                            int bs1 = cycleDay & 0xFF;          // Day of Month
                            int bs2 = (cycleDay >> 8) & 0xFF;   // Month
                            int bs3 = (cycleDay >> 16) & 0xFF;  // num Days
                            XposedBridge.log("HOOK pref LOAD BITSHIFT "+bs1+"."+bs2+"."+bs3+"");
                            pref_cycle_date.set(Calendar.DAY_OF_MONTH, bs1);
                            pref_cycle_date.set(Calendar.MONTH, bs2);
                            if(pref_cycle_date.getTimeInMillis() > System.currentTimeMillis()) {
                                pref_cycle_date.set(Calendar.YEAR, pref_cycle_date.get(Calendar.YEAR) - 1);
                                XposedBridge.log("HOOK pref year set to "+pref_cycle_date.get(Calendar.YEAR)+"");
                            }
                            pref_cycle_days = bs3;
                        } catch (ClassCastException e){
                            //Not a viewGroup here
                            XposedBridge.log("HOOK pref decoding error "+ e.toString() );
                        }
                    }

                    //Update pref_cycle_date to Last Cycle
                    while (pref_cycle_date.getTimeInMillis() < System.currentTimeMillis()) {     // Cycle until cycle_date > currentTime
                        pref_cycle_date.add(Calendar.DAY_OF_MONTH, pref_cycle_days);
                        XposedBridge.log("HOOK pref pref_cycle_date update to "+pref_cycle_date+"");
                    }
                    pref_cycle_date.add(Calendar.DAY_OF_MONTH, -pref_cycle_days);                       //Set Last Cycle


                    final NumberPicker cycleDayPicker = (NumberPicker) view.findViewById(R_id_cycle_day);
                    XposedBridge.log("HOOK Numberpicker = "+cycleDayPicker.getId() + " " +cycleDayPicker.toString());
                    cycleDayPicker.setMinValue(1);
                    cycleDayPicker.setMaxValue(31);
                    cycleDayPicker.setValue(cycleDay);
                    cycleDayPicker.setWrapSelectorWheel(true);

                    final NumberPicker cycleDaysPicker = (NumberPicker) view.findViewById(R_id_cycle_days);
                    String[] values = new String[100]; for (int i = 0; i < 100; ++i) {values[i] = ""+(i+1);}
                    values[0] = context.getString(modR_strings_nr1_daily);
                    values[6] = context.getString(modR_strings_nr7_weekly);
                    values[29] = context.getString(modR_strings_nr30_fixedmonth);
                    values[30] = context.getString(modR_strings_nr31_monthly);
                    cycleDaysPicker.setDisplayedValues( values );
                    cycleDaysPicker.setMinValue(1);
                    cycleDaysPicker.setMaxValue(100);
                    cycleDaysPicker.setValue(pref_cycle_days);
                    cycleDaysPicker.setWrapSelectorWheel(true);

                    final DatePicker cycleDatePicker = (DatePicker) view.findViewById(R_id_datepicker);
                    int year=pref_cycle_date.get(Calendar.YEAR);
                    int month=pref_cycle_date.get(Calendar.MONTH);
                    int day=pref_cycle_date.get(Calendar.DAY_OF_MONTH);
                    cycleDatePicker.updateDate(year, month, day);
                    cycleDatePicker.setMaxDate(System.currentTimeMillis());

                    builder.setPositiveButton("OK",             //R.string.data_usage_cycle_editor_positive
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // clear focus to finish pending text edits
                                    cycleDayPicker.clearFocus();
                                    cycleDaysPicker.clearFocus();
                                    cycleDatePicker.clearFocus();

                                    //BitShift pref into int
                                    int bs1 = cycleDatePicker.getDayOfMonth();
                                    int bs2 = cycleDatePicker.getMonth();
                                    int bs3 = cycleDaysPicker.getValue();
                                    int bs = (bs1 & 0xFF) | ((bs2 & 0xFF) << 8) | ((bs3 & 0xFF) << 16);
                                    XposedBridge.log("HOOK pref SAVED "+bs+" ("+bs1+"."+bs2+"."+bs3+")");

                                    //Save in policy CycleDay
                                    final String cycleTimezone = new Time().timezone;
                                    XposedHelpers.callMethod(editor,"setPolicyCycleDay", template, bs, cycleTimezone);
                                    XposedHelpers.callMethod(target,"updatePolicy", true);

                                }
                            });

                    XposedBridge.log("HOOK onCreateDialog END!");

                    return builder.create();

                }

            });

            return;
        }

    }





    /****************************
     Service static Methods

     */

    public static void view_dump(View view){
        XposedBridge.log( "HOOK DUMP view " + view.toString());
        try {
            ViewGroup rootView = (ViewGroup) view.getRootView();
            int childViewCount = rootView.getChildCount();
            for (int i=0; i<childViewCount;i++){
                View workWithMe = rootView.getChildAt(i);
                XposedBridge.log( "HOOK DUMP view found {" + workWithMe.getId()+"} : "+ workWithMe.toString() + " " );
                if(workWithMe instanceof LinearLayout ){
                    XposedBridge.log( "HOOK DUMP             linearLayout contains:" );
                    LinearLayout llworkWithme = (LinearLayout) workWithMe;
                    int llchildViewCount = llworkWithme.getChildCount();
                    for (int lli=0; lli<llchildViewCount;lli++){
                        View llview = llworkWithme.getChildAt(lli);
                        XposedBridge.log( "HOOK DUMP              + {" + llview.getId()+"} : "+ llview.toString() + " " );
                    }
                }
            }
        } catch (ClassCastException e){
            //Not a viewGroup here
            XposedBridge.log("HOOK DUMP view exception  Not a viewGroup here "+ e.toString() );
        } catch (NullPointerException e){
            //Root view is null
            XposedBridge.log("HOOK DUMP view exception  Root view is null "+ e.toString() );
        }
    }

    public static int daysBetween(long day1, long day2){
        Calendar dayOne = Calendar.getInstance();
        dayOne.setTimeInMillis(day1);
        Calendar dayTwo = Calendar.getInstance();
        dayTwo.setTimeInMillis(day2);

        if (dayOne.get(Calendar.YEAR) == dayTwo.get(Calendar.YEAR)) {
            return Math.abs(dayOne.get(Calendar.DAY_OF_YEAR) - dayTwo.get(Calendar.DAY_OF_YEAR));
        } else {
            if (dayTwo.get(Calendar.YEAR) > dayOne.get(Calendar.YEAR)) {
                //swap them
                Calendar temp = dayOne;
                dayOne = dayTwo;
                dayTwo = temp;
            }
            int extraDays = 0;

            int dayOneOriginalYearDays = dayOne.get(Calendar.DAY_OF_YEAR);

            while (dayOne.get(Calendar.YEAR) > dayTwo.get(Calendar.YEAR)) {
                dayOne.add(Calendar.YEAR, -1);
                // getActualMaximum() important for leap years
                extraDays += dayOne.getActualMaximum(Calendar.DAY_OF_YEAR);
            }

            return extraDays - dayTwo.get(Calendar.DAY_OF_YEAR) + dayOneOriginalYearDays ;
        }
    }

}


