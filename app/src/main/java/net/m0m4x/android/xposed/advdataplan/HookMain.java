package net.m0m4x.android.xposed.advdataplan;
/**
 * Created by max on 09/04/2017.
 */

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.XModuleResources;
import android.os.Build;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class HookMain implements IXposedHookZygoteInit, IXposedHookInitPackageResources, IXposedHookLoadPackage {

    private static final boolean DEBUG = BuildConfig.enableDebug;

    /****************************
        RESOURCES Hooking

     */

    private static String MODULE_PATH = null;


    static int R_layout_data_usage_cycle_editor;
    static int R25_xml_data_usage_cellular;
    static int R_id_datepicker;
    static int R_id_cycle_days;
    static int R_id_cycle_day;

    static int modR_strings_dataplan_days;
    static int modR_strings_dataplan_day;
    static int modR_strings_nr1_daily;
    static int modR_strings_nr7_weekly;
    static int modR_strings_nr30_fixedmonth;
    static int modR_strings_nr31_monthly;
    static int modR_strings_summary_days;
    static int modR_strings_summary_starting;
    static int modR_strings_cycle_days;
    static int modR_strings_cycle_detail;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        MODULE_PATH = startupParam.modulePath;
        if(DEBUG) XposedBridge.log("HOOK init AdvDataPlan - modulePath:" + startupParam.modulePath + " sdk: "+Build.VERSION.SDK_INT+"");

    }

    @Override
    public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
        if(DEBUG) XposedBridge.log("HOOK RES init -  " + resparam.packageName + " ! ");
        if(!resparam.packageName.equals("com.android.settings")) {
            return;
        }

        /*
            Get ID of module resources

            Marshmallow: native
            Nougat: Ok!
         */
        XModuleResources modRes = XModuleResources.createInstance(MODULE_PATH, resparam.res);
        modR_strings_dataplan_days = resparam.res.addResource(modRes, R.string.dataplan_days);
        modR_strings_dataplan_day = resparam.res.addResource(modRes, R.string.dataplan_day);
        modR_strings_nr1_daily = resparam.res.addResource(modRes, R.string.nr1_daily);
        modR_strings_nr7_weekly = resparam.res.addResource(modRes, R.string.nr7_weekly);
        modR_strings_nr30_fixedmonth = resparam.res.addResource(modRes, R.string.nr30_fixedmonth);
        modR_strings_nr31_monthly = resparam.res.addResource(modRes, R.string.nr31_monthly);
        modR_strings_summary_days = resparam.res.addResource(modRes, R.string.summary_days);
        modR_strings_summary_starting = resparam.res.addResource(modRes, R.string.summary_starting);
        modR_strings_cycle_days = resparam.res.addResource(modRes, R.string.cycle_days);
        modR_strings_cycle_detail = resparam.res.addResource(modRes, R.string.cycle_detail);

        /*
            Get ID of native resources

            Marshmallow: native
            Nougat: Ok!
         */
        R_layout_data_usage_cycle_editor = resparam.res.getIdentifier("data_usage_cycle_editor", "layout", "com.android.settings");
        if(DEBUG) XposedBridge.log("HOOK RES       ...found R.layout.data_usage_cycle_editor : " + R_layout_data_usage_cycle_editor + " !");

        R25_xml_data_usage_cellular = resparam.res.getIdentifier("data_usage_cellular", "xml", "com.android.settings");
        if(DEBUG) XposedBridge.log("HOOK RES       ...found R.layout.data_usage_cellular : " + R25_xml_data_usage_cellular + " !");

        /*
            Hook Layout 'data_usage_cycle_editor'

            Marshmallow: native
            Nougat: Ok!
        */
        resparam.res.hookLayout("com.android.settings", "layout", "data_usage_cycle_editor", new XC_LayoutInflated() {
            @SuppressLint("NewApi")
            @Override
            public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                if(DEBUG) XposedBridge.log("HOOK RES handleLayoutInflated(): Layout is inflating... - data_usage_cycle_editor!");

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

                try {

                    //numberPicker
                    R_id_cycle_day = liparam.res.getIdentifier("cycle_day", "id", "com.android.settings");
                    Object l0_num =  liparam.view.findViewById(R_id_cycle_day);
                    XposedHelpers.callMethod(l0_num, "setVisibility", View.GONE);

                    //debug
                    //if (DEBUG) view_dump(l0_num);

                    //layout 0 (root - ViewGroup)
                    ViewGroup res_layout0 = (ViewGroup) XposedHelpers.callMethod(l0_num, "getParent");
                    if(DEBUG) XposedBridge.log("HOOK RES handleLayoutInflated(): Parent layout is " + res_layout0.getClass().getName());
                    //case: LinearLayout - set Vertical
                    if (res_layout0 instanceof LinearLayout) {
                        if(DEBUG) XposedBridge.log("HOOK RES handleLayoutInflated(): LinearLayout instance detected.");
                        LinearLayout res_lin_layout0 = (LinearLayout) res_layout0;
                        res_lin_layout0.setOrientation(LinearLayout.VERTICAL);
                    }

                    //hide originals layouts
                    try {
                        for (int i = 0; i < res_layout0.getChildCount(); i++) {
                            res_layout0.getChildAt(i).setVisibility(View.GONE);
                        }
                    } catch(Exception ex) {
                        XposedBridge.log("HOOK WARNING handleLayoutInflated(): Cannot empty existent layout!");
                        StackTraceElement[] elements = ex.getStackTrace();
                        XposedBridge.log("HOOK WARNING handleLayoutInflated(): " + ex.getMessage());
                        XposedBridge.log("HOOK WARNING handleLayoutInflated(): at " + elements[0].getClassName() + "." + elements[0].getMethodName() + "() line: " + elements[0].getLineNumber());
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
                    DatePickerDialog l1_dat = new DatePickerDialog(context, android.R.style.Theme_Holo_Light_Dialog, null, 2017,4,16);
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
                    (res_layout0).addView(res_layout1);
                    (res_layout0).addView(res_layout2);

                } catch (Exception ex) {
                    XposedBridge.log("HOOK ERROR handleLayoutInflated(): " + ex.getMessage());
                    StackTraceElement[] elements = ex.getStackTrace();
                    XposedBridge.log("HOOK ERROR handleLayoutInflated(): at " + elements[0].getClassName() + "." + elements[0].getMethodName() + "() line: " + elements[0].getLineNumber());
                    throw ex;
                }

                if(DEBUG) XposedBridge.log("HOOK RES handleLayoutInflated(): Layout is inflated!");
            }
        });

    }





    /****************************
        METHODS Hooking

     */

    public static final int CYCLE_NONE = -1;
    private static final String EXTRA_TEMPLATE = "template";

    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {

        if(DEBUG) XposedBridge.log("HOOK handleLoadPackage - " + lpparam.packageName + "! " );

        /*
            NetworkPolicy Classes - Compute cycle Boundaries

         */
        if(         lpparam.packageName.equals("android")
                ||  lpparam.packageName.equals("com.android.systemui")
                ||  lpparam.packageName.equals("com.android.settings")
                ||  lpparam.packageName.equals("com.android.providers.settings")
                ){
            if(DEBUG) XposedBridge.log("HOOK NetworkPolicyManager methods! (pkg:"+lpparam.packageName+")");

            final Class<?> NetworkPolicyManager = XposedHelpers.findClass(
                    "android.net.NetworkPolicyManager",
                    lpparam.classLoader);
            final Class<?> NetworkPolicy = XposedHelpers.findClass(
                    "android.net.NetworkPolicy",
                    lpparam.classLoader);

            if(Build.VERSION.SDK_INT == 24 || Build.VERSION.SDK_INT == 25){

                findAndHookMethod(NetworkPolicyManager, "computeNextCycleBoundary", long.class , NetworkPolicy , new XC_MethodReplacement() {

                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        if(DEBUG) XposedBridge.log("HOOK REQ computeNextCycleBoundary(): (pkg:"+lpparam.packageName+")");

                        // Get Params
                        long currentTime = (long) param.args[0];    // long currentTime
                        Object policy = param.args[1];              // NetworkPolicy policy
                        int cycle_day = (int) XposedHelpers.getObjectField(policy, "cycleDay");

                        // Check
                        if (cycle_day == CYCLE_NONE) {
                            throw new IllegalArgumentException("Unable to compute boundary without cycleDay");
                        }

                        return mComputeNextCycleBoundary(currentTime, cycle_day);

                    }

                });

                findAndHookMethod(NetworkPolicyManager, "computeLastCycleBoundary", long.class , NetworkPolicy , new XC_MethodReplacement() {

                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        if(DEBUG) XposedBridge.log("HOOK REQ computeLastCycleBoundary(): (pkg:"+lpparam.packageName+")");

                        // Get Params
                        long currentTime = (long) param.args[0];    // long currentTime
                        Object policy = param.args[1];              // NetworkPolicy policy
                        int cycle_day = (int) XposedHelpers.getObjectField(policy, "cycleDay");

                        // Need to Check policy template mobile? (otherwise do default)

                        // Check
                        if (cycle_day == CYCLE_NONE) {
                            throw new IllegalArgumentException("Unable to compute boundary without cycleDay");
                        }
                        return mComputeLastCycleBoundary(currentTime, cycle_day);

                    }

                });

            } else if (Build.VERSION.SDK_INT == 26 || Build.VERSION.SDK_INT == 27) {

                final Class<?> RecurrenceRule = XposedHelpers.findClass(
                        "android.util.RecurrenceRule",
                        lpparam.classLoader);

                final Class<?> ZoneIdClass = XposedHelpers.findClass(
                        "java.time.ZoneId",
                        lpparam.classLoader);

                findAndHookMethod(NetworkPolicy, "buildRule", int.class , ZoneIdClass , new XC_MethodReplacement() {

                    @Override
                    @TargetApi(26)
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        if(DEBUG) XposedBridge.log("HOOK REQ buildRule(): (pkg:"+lpparam.packageName+")");

                        // Get Params
                        int cycleDay = (int) param.args[0];
                        ZoneId cycleTimezone = (ZoneId) param.args[1];              // ZoneId
                        if(DEBUG) XposedBridge.log("HOOK REQ buildRule() cycleday: "+cycleDay+")");

                        int cycle_none = XposedHelpers.getStaticIntField(NetworkPolicy, "CYCLE_NONE");
                        if (cycleDay != cycle_none) {

                            //Decode CycleDay
                            Object[] decodedArr = decodeBitShiftedInt(cycleDay);
                            Calendar pref_cycleDate = (Calendar) decodedArr[0];
                            int pref_cycleDays = (int) decodedArr[1];

                            //Get int of days
                            int year = pref_cycleDate.get(Calendar.YEAR);
                            int month = pref_cycleDate.get(Calendar.MONTH);
                            int day = pref_cycleDate.get(Calendar.DAY_OF_MONTH);

                            Clock sClock = (Clock) XposedHelpers.getStaticObjectField(RecurrenceRule, "sClock");
                            final ZonedDateTime now = ZonedDateTime.now(sClock).withZoneSameInstant(cycleTimezone);

                            if(DEBUG) XposedBridge.log("HOOK BUILD RULE From Date "+year+" "+month+" "+day+" * " + pref_cycleDays + " days ");

                            final ZonedDateTime start;
                            if (pref_cycleDays != 31) {
                                // we add 1 to the month value, as Calendar class returns value brginning from 0 for January, while ZonedDateTime has the value 1 for January and so on
                                start = ZonedDateTime.of(
                                        now.toLocalDate().withMonth(month+1).withDayOfMonth(day),
                                        LocalTime.MIDNIGHT, cycleTimezone);

                                return XposedHelpers.newInstance(RecurrenceRule , start, null, Period.ofDays(pref_cycleDays) );
                            } else {
                                start = ZonedDateTime.of(
                                        now.toLocalDate().minusYears(1).withMonth(1).withDayOfMonth(day),
                                        LocalTime.MIDNIGHT, cycleTimezone);

                                return XposedHelpers.newInstance(RecurrenceRule , start, null, Period.ofMonths(1) );
                            }

                        } else {
                            if(DEBUG) XposedBridge.log("HOOK BUILD RULE Returned Never! ");
                            return XposedHelpers.callStaticMethod(RecurrenceRule, "buildNever");
                        }

                    }

                });

                final Class<?> NetworkPolicyEditor = XposedHelpers.findClassIfExists(
                        "com.android.settingslib.NetworkPolicyEditor",
                        lpparam.classLoader);
                if(NetworkPolicyEditor != null) {

                    final Class<?> NetworkTemplate = XposedHelpers.findClass(
                            "android.net.NetworkTemplate",
                            lpparam.classLoader);

                    findAndHookMethod(NetworkPolicyEditor, "getPolicyCycleDay", NetworkTemplate , new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            if(DEBUG) XposedBridge.log("HOOK REQ getPolicyCycleDay(): (pkg:"+lpparam.packageName+")");
                            Object template = param.args[0];
                            Object policy = XposedHelpers.callMethod(param.thisObject, "getPolicy", template);
                            if (policy != null) {
                                Object cycleRule = XposedHelpers.getObjectField(policy, "cycleRule");
                                if ((boolean) XposedHelpers.callMethod(cycleRule,"isMonthly")){
                                    if(DEBUG) XposedBridge.log("HOOK REQ getPolicyCycleDay(): isMonthly! ");
                                    Object start = XposedHelpers.getObjectField(cycleRule,"start");
                                    return (Object) XposedHelpers.callMethod(start,"getDayOfMonth");
                                } else {
                                    if(DEBUG) XposedBridge.log("HOOK REQ getPolicyCycleDay(): is NOT Monthly! ");
                                    Object start = XposedHelpers.getObjectField(cycleRule,"start");
                                    Object start_month = XposedHelpers.callMethod(start,"getMonth");
                                    Object period = XposedHelpers.getObjectField(cycleRule,"period");
                                    int encoded = encodeBitShiftedInt( (int) XposedHelpers.callMethod(start,"getDayOfMonth"),
                                            (int) XposedHelpers.callMethod(start_month,"getValue")-1,
                                            (int) XposedHelpers.callMethod(period,"getDays"));
                                    if(DEBUG) XposedBridge.log("HOOK REQ getPolicyCycleDay(): returning "+encoded +" ");
                                    return encoded;
                                }
                            } else {
                                return CYCLE_NONE;
                            }
                        }
                    });

                }

            }

        }

        /*
            DataUsage Classes - Editor Dialog Fragment

         */
        if( lpparam.packageName.equals("com.android.settings") ) {
            if(DEBUG) XposedBridge.log("HOOK DataUsageSummary/BillingCycleSettings methods! (pkg:"+lpparam.packageName+")!");

            //SDK23
            if (Build.VERSION.SDK_INT == 23) {
                final Class<?> CycleEditorFragment = XposedHelpers.findClass(
                        "com.android.settings.DataUsageSummary.CycleEditorFragment",
                        lpparam.classLoader);
                findAndHookMethod(CycleEditorFragment, "onCreateDialog", "android.os.Bundle", new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        if (DEBUG) XposedBridge.log("HOOK onCreateDialog(): Called now!");

                        try {
                            return createAdvDialog(param);
                        } catch (Exception ex) {
                            XposedBridge.log("HOOK ERROR createAdvDialog(): " + ex.getMessage());
                            StackTraceElement[] elements = ex.getStackTrace();
                            XposedBridge.log("HOOK ERROR createAdvDialog(): at " + elements[0].getClassName() + "." + elements[0].getMethodName() + "() line: " + elements[0].getLineNumber());
                            String dump=obj_dump(param.thisObject);
                            XposedBridge.log("HOOK ERROR createAdvDialog() objDUMP: Dumping DataUsageSummary.CycleEditorFragment ...");
                            for (String item : dump.split(System.getProperty("line.separator"))) {
                                XposedBridge.log("HOOK ERROR createAdvDialog() objDUMP: " + item);
                            }
                            throw ex;
                        }

                    }

                });
            }

            //SDK24-25-26-27
            if (Build.VERSION.SDK_INT == 24 || Build.VERSION.SDK_INT == 25 || Build.VERSION.SDK_INT == 26 || Build.VERSION.SDK_INT == 27) {
                final Class<?> CycleEditorFragment = XposedHelpers.findClass(
                        "com.android.settings.datausage.BillingCycleSettings.CycleEditorFragment",
                        lpparam.classLoader);
                findAndHookMethod(CycleEditorFragment, "onCreateDialog", "android.os.Bundle", new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        if (DEBUG) XposedBridge.log("HOOK onCreateDialog(): Starting here! " + lpparam.packageName);

                        try {
                            return createAdvDialog(param);
                        } catch (Exception ex) {
                            XposedBridge.log("HOOK ERROR createAdvDialog(): " + ex.getMessage());
                            StackTraceElement[] elements = ex.getStackTrace();
                            XposedBridge.log("HOOK ERROR createAdvDialog(): at " + elements[0].getClassName() + "." + elements[0].getMethodName() + "() line: " + elements[0].getLineNumber());
                            String dump=obj_dump(param.thisObject);
                            XposedBridge.log("HOOK ERROR createAdvDialog() objDUMP: Dumping BillingCycleSettings.CycleEditorFragment ...");
                            for (String item : dump.split(System.getProperty("line.separator"))) {
                                XposedBridge.log("HOOK ERROR createAdvDialog() objDUMP: " + item);
                            }
                            throw ex;
                        }

                    }
                });
                findAndHookMethod(CycleEditorFragment, "onClick", DialogInterface.class, int.class , new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        if (DEBUG) XposedBridge.log("HOOK onClick START!" + lpparam.packageName);

                        try {
                            final Object args = XposedHelpers.callMethod(param.thisObject, "getArguments");
                            final Object template = XposedHelpers.callMethod(args, "getParcelable", EXTRA_TEMPLATE );       //type NetworkTemplate
                            final Object target = XposedHelpers.callMethod(param.thisObject, "getTargetFragment");          //type sdk24:BillingCycleSettings sdk25:DataUsageEditController

                            final Object editor;                                                                            //type NetworkPolicyEditor
                            if (Build.VERSION.SDK_INT == 24) {
                                Object services = XposedHelpers.getObjectField(target,"services");
                                editor = XposedHelpers.getObjectField(services,"mPolicyEditor");
                            } else if (Build.VERSION.SDK_INT == 25 || Build.VERSION.SDK_INT == 26 || Build.VERSION.SDK_INT == 27) {
                                editor = XposedHelpers.callMethod(target, "getNetworkPolicyEditor");
                            } else {
                                XposedBridge.log("HOOK AdvDialog.onClick(): SDK "+Build.VERSION.SDK_INT+" not supported!");
                                return null;
                            }

                            final NumberPicker cycleDayPicker = (NumberPicker) XposedHelpers.getObjectField(param.thisObject, "mCycleDayPicker");
                            final NumberPicker cycleDaysPicker = (NumberPicker) XposedHelpers.getAdditionalStaticField(param.thisObject, "mCycleDaysPicker");
                            final DatePicker cycleDatePicker = (DatePicker) XposedHelpers.getAdditionalStaticField(param.thisObject, "mCycleDatePicker");

                            // clear focus to finish pending text edits
                            cycleDayPicker.clearFocus();
                            cycleDaysPicker.clearFocus();
                            cycleDatePicker.clearFocus();

                            // Encode Day of Month, Month and Duration into one int
                            // via BitShift method.
                            int bs = encodeBitShiftedIntFromPickers(cycleDatePicker, cycleDaysPicker);

                            //Save in policy CycleDay
                            final String cycleTimezone = new Time().timezone;
                            XposedHelpers.callMethod(editor, "setPolicyCycleDay", template, bs, cycleTimezone);

                            if (Build.VERSION.SDK_INT == 24) {
                                XposedHelpers.callMethod(target, "updatePrefs");
                            } else if (Build.VERSION.SDK_INT == 25 | Build.VERSION.SDK_INT == 26 | Build.VERSION.SDK_INT == 27) {
                                XposedHelpers.callMethod(target, "updateDataUsage");
                            }

                            return null;

                        } catch (Exception ex) {
                            XposedBridge.log("HOOK ERROR AdvDialog.onClick(): " + ex.getMessage());
                            StackTraceElement[] elements = ex.getStackTrace();
                            XposedBridge.log("HOOK ERROR AdvDialog.onClick(): at " + elements[0].getClassName() + "." + elements[0].getMethodName() + "() line: " + elements[0].getLineNumber());
                            String dump=obj_dump(param.thisObject);
                            XposedBridge.log("HOOK ERROR AdvDialog.onClick() objDUMP: Dumping BillingCycleSettings.CycleEditorFragment ...");
                            for (String item : dump.split(System.getProperty("line.separator"))) {
                                XposedBridge.log("HOOK ERROR AdvDialog.onClick() objDUMP: " + item);
                            }
                            throw ex;
                        }

                    }
                });

            }

        }

        /*
            System UI - getDataUsageInfo

         */

        if(         lpparam.packageName.equals("com.android.systemui")
                ||  lpparam.packageName.equals("com.android.settings")
                ){

            if(DEBUG) XposedBridge.log("HOOK SystemUI methods! (pkg:"+lpparam.packageName+")!");

            //SDK21-22 testing
            //SDK23
            if (Build.VERSION.SDK_INT == 21 || Build.VERSION.SDK_INT == 22 || Build.VERSION.SDK_INT == 23) {
                final Class<?> MobileDataController = XposedHelpers.findClassIfExists(
                        "com.android.systemui.statusbar.policy.MobileDataControllerImpl",
                        lpparam.classLoader);
                if(MobileDataController != null) {
                    final Class<?> NetworkTemplate = XposedHelpers.findClass(
                            "android.net.NetworkTemplate",
                            lpparam.classLoader);
                    final Class<?> NetworkStatsHistory = XposedHelpers.findClass(
                            "android.net.NetworkStatsHistory",
                            lpparam.classLoader);
                    final Class<?> DataUsageInfo = XposedHelpers.findClass(
                            "com.android.systemui.statusbar.policy.NetworkController$MobileDataController$DataUsageInfo",
                            lpparam.classLoader);
                    findAndHookMethod(MobileDataController, "getDataUsageInfo", new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            if (DEBUG)
                                XposedBridge.log("HOOK UI MobileDataController.getDataUsageInfo(): (pkg:" + lpparam.packageName + ")");

                            return getAdvDataUsageInfo(param, NetworkTemplate, NetworkStatsHistory, DataUsageInfo, null);
                        }
                    });
                }
            }


            //SDK24-25
            //SDK26-27  Hook not needed!
            if (Build.VERSION.SDK_INT == 24 || Build.VERSION.SDK_INT == 25) {
                final Class<?> DataUsageController = XposedHelpers.findClassIfExists(
                        "com.android.settingslib.net.DataUsageController",
                        lpparam.classLoader);
                if(DataUsageController != null) {
                    final Class<?> NetworkPolicyManager = XposedHelpers.findClass(
                            "android.net.NetworkPolicyManager",
                            lpparam.classLoader);
                    final Class<?> NetworkTemplate = XposedHelpers.findClass(
                            "android.net.NetworkTemplate",
                            lpparam.classLoader);
                    final Class<?> NetworkStatsHistory = XposedHelpers.findClass(
                            "android.net.NetworkStatsHistory",
                            lpparam.classLoader);
                    final Class<?> DataUsageInfo = XposedHelpers.findClass(
                            "com.android.settingslib.net.DataUsageController.DataUsageInfo",
                            lpparam.classLoader);
                    findAndHookMethod(DataUsageController, "getDataUsageInfo", NetworkTemplate, new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            if (DEBUG)
                                XposedBridge.log("HOOK UI DataUsageController.getDataUsageInfo(): (pkg:" + lpparam.packageName + ")");

                            return getAdvDataUsageInfo(param, NetworkTemplate, NetworkStatsHistory, DataUsageInfo, NetworkPolicyManager);
                        }
                    });
                }

            }

        }


         /*
            System UI - Settings App - Billing Cycle preview
            only in SDK 24-25-26-27
         */
        if (Build.VERSION.SDK_INT == 24 || Build.VERSION.SDK_INT == 25 || Build.VERSION.SDK_INT == 26 || Build.VERSION.SDK_INT == 27 ) {
            if(   false||   lpparam.packageName.equals("com.android.settings")
                       ||   lpparam.packageName.equals("com.android.settings.datausage")
                    ) {

                //BillingCyclePreference.java
                // com.android.settings.datausage.BillingCyclePreference
                // extend android.support.v7.preference.Preference
                final Class<?> Preferencev7 = XposedHelpers.findClass(
                        "android.support.v7.preference.Preference",
                        lpparam.classLoader);
                findAndHookMethod(Preferencev7, "setSummary", CharSequence.class, new XC_MethodHook() {
                    @Override
                    @TargetApi(26)
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (DEBUG)XposedBridge.log("HOOK UI Preference.setSummary(): class "+param.thisObject.getClass().getName().toString()+" (pkg:" + lpparam.packageName + ")");

                        //com.android.settings.datausage.BillingCyclePreference
                        if (param.thisObject.getClass().getName().equals("com.android.settings.datausage.BillingCyclePreference")) {

                            if (DEBUG) XposedBridge.log("HOOK UI Preferences! ;)");
                            try {

                                //Get CycleDay
                                int cycleDay = 1;
                                if (Build.VERSION.SDK_INT == 24 || Build.VERSION.SDK_INT == 25) {
                                    //SDK 24-25
                                    Object policy = XposedHelpers.getObjectField(param.thisObject, "mPolicy");
                                    if (policy != null)
                                        cycleDay = (int) XposedHelpers.getObjectField(policy, "cycleDay");
                                } else if (Build.VERSION.SDK_INT == 26 || Build.VERSION.SDK_INT == 27) {
                                    //SDK 26-27
                                    Object services = XposedHelpers.getObjectField(param.thisObject, "mServices");
                                    Object editor = XposedHelpers.getObjectField(services, "mPolicyEditor");
                                    Object template = XposedHelpers.getObjectField(param.thisObject, "mTemplate");
                                    cycleDay = (int) XposedHelpers.callMethod(editor, "getPolicyCycleDay", template);
                                } else {
                                    if (DEBUG) XposedBridge.log("HOOK UI Preference.setSummary(): Api "+Build.VERSION.SDK_INT+" not supported!");
                                    return;
                                }

                                //Decode CycleDay
                                Object[] decodedArr = decodeBitShiftedInt(cycleDay);
                                Calendar pref_cycleDate = (Calendar) decodedArr[0];
                                int pref_cycleDays = (int) decodedArr[1];

                                //Build phrase
                                final Context context = (Context) XposedHelpers.callMethod(param.thisObject, "getContext");
                                String strDays;
                                switch (pref_cycleDays) {
                                    case 1:
                                        strDays = (String) XposedHelpers.callMethod(context, "getString", modR_strings_nr1_daily);
                                        break;
                                    case 7:
                                        strDays = (String) XposedHelpers.callMethod(context, "getString", modR_strings_nr7_weekly);
                                        break;
                                    case 31:
                                        strDays = (String) XposedHelpers.callMethod(context, "getString", modR_strings_nr31_monthly);
                                        break;
                                    default:
                                        strDays = String.format((String) XposedHelpers.callMethod(context, "getString", modR_strings_summary_days), pref_cycleDays);
                                        break;
                                }

                                Format format = new SimpleDateFormat("dd MMM yyyy");
                                param.args[0] = strDays + " " + String.format((String) XposedHelpers.callMethod(context, "getString", modR_strings_summary_starting), format.format(new Date(pref_cycleDate.getTimeInMillis())));

                            } catch (Exception ex) {
                                XposedBridge.log("HOOK ERROR Preference.setSummary(): " + ex.getMessage());
                                StackTraceElement[] elements = ex.getStackTrace();
                                XposedBridge.log("HOOK ERROR Preference.setSummary(): at " + elements[0].getClassName() + "." + elements[0].getMethodName() + "() line: " + elements[0].getLineNumber());
                                String dump = obj_dump(param.thisObject);
                                XposedBridge.log("HOOK ERROR Preference.setSummary() objDUMP: Dumping Preferencev7 ...");
                                for (String item : dump.split(System.getProperty("line.separator"))) {
                                    XposedBridge.log("HOOK ERROR Preference.setSummary() objDUMP: " + item);
                                }
                                throw ex;
                            }
                        }

                    }
                });

                //BillingCycleSettings.java
                // com.android.settings.datausage.BillingCycleSettings
                // calls android.support.v7.preference.Preference setSummary()
                final Class<?> BillingCycleSettings = XposedHelpers.findClass(
                        "com.android.settings.datausage.BillingCycleSettings",
                        lpparam.classLoader);
                findAndHookMethod(BillingCycleSettings, "updatePrefs", new XC_MethodHook() {
                    @Override
                    @TargetApi(26)
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (DEBUG)XposedBridge.log("HOOK UI BillingCycleSettings.updatePrefs(): (pkg:" + lpparam.packageName + ")");

                        try {
                            Object template = XposedHelpers.getObjectField(param.thisObject,"mNetworkTemplate");
                            Object services = XposedHelpers.getObjectField(param.thisObject,"services");
                            Object editor = XposedHelpers.getObjectField(services,"mPolicyEditor");
                            Object policy = XposedHelpers.callMethod(editor,"getPolicy",template);

                            //Get CycleDay
                            int cycleDay = 1;
                            int pref_cycleDays = 31;
                            Calendar pref_cycleDate = null;
                            Calendar pref_cycleDateEnd = null;
                            if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT <= 25) {
                                if(policy != null) cycleDay = (int) XposedHelpers.getObjectField(policy, "cycleDay");

                                //Decode CycleDay
                                Object[] decodedArr = decodeBitShiftedInt(cycleDay);
                                pref_cycleDate = (Calendar) decodedArr[0];
                                pref_cycleDateEnd = Calendar.getInstance();
                                pref_cycleDays = (int) decodedArr[1];
                                pref_cycleDateEnd.setTimeInMillis(mComputeNextCycleBoundary(pref_cycleDate.getTimeInMillis(), cycleDay));

                            } else if (Build.VERSION.SDK_INT == 26 || Build.VERSION.SDK_INT == 27) {
                                cycleDay = (int) XposedHelpers.callMethod(editor, "getPolicyCycleDay", template);		// policy.cycleRule.start.getDayOfMonth()

                                pref_cycleDate = Calendar.getInstance();
                                pref_cycleDateEnd = Calendar.getInstance();

                                if (policy != null) {
                                    final Object mCycleIterator = XposedHelpers.callMethod(policy, "cycleIterator");
                                    final Object mCycle = (Object) XposedHelpers.callMethod(mCycleIterator, "next");

                                    ZonedDateTime start = (ZonedDateTime) XposedHelpers.getObjectField(mCycle, "first");
                                    ZonedDateTime end = (ZonedDateTime) XposedHelpers.getObjectField(mCycle, "second");
                                    pref_cycleDate.setTimeInMillis(start.toInstant().toEpochMilli());
                                    pref_cycleDateEnd.setTimeInMillis(end.toInstant().toEpochMilli());

                                    Object mCycleRule = (Object) XposedHelpers.getObjectField(policy, "cycleRule");
                                    Object mPeriod = (Object) XposedHelpers.getObjectField(mCycleRule, "period");

                                    int cycleDays = (int) XposedHelpers.callMethod(mPeriod, "getDays");

                                    if (cycleDays > 0)
                                        pref_cycleDays = cycleDays;
                                }
                            } else {
                                if (DEBUG) XposedBridge.log("HOOK UI Preference.setSummary(): Api "+Build.VERSION.SDK_INT+" not supported!");
                                return;
                            }
                            if (DEBUG)XposedBridge.log("HOOK UI BillingCycleSettings.updatePrefs(): (pkg:" + lpparam.packageName + ")");


                            //Build phrase
                            final Context context = (Context) XposedHelpers.callMethod(param.thisObject, "getContext");
                            String strDays;
                            switch (pref_cycleDays) {
                                case 1:  strDays = (String) XposedHelpers.callMethod(context, "getString", modR_strings_nr1_daily);
                                    break;
                                case 7:  strDays = (String) XposedHelpers.callMethod(context, "getString", modR_strings_nr7_weekly);
                                    break;
                                case 31:  strDays = (String) XposedHelpers.callMethod(context, "getString", modR_strings_nr31_monthly);
                                    break;
                                default: strDays = String.format((String) XposedHelpers.callMethod(context, "getString", modR_strings_cycle_days), Integer.toString(pref_cycleDays));
                                    break;
                            }

                            Format format = new SimpleDateFormat("dd MMM");
                            Object pref = XposedHelpers.getObjectField(param.thisObject, "mBillingCycle");
                            XposedHelpers.callMethod(pref, "setSummary", String.format((String) XposedHelpers.callMethod(context, "getString", modR_strings_cycle_detail), format.format(new Date(pref_cycleDate.getTimeInMillis())), format.format(new Date(pref_cycleDateEnd.getTimeInMillis())), strDays.toLowerCase() ));

                        } catch (Exception ex) {
                            XposedBridge.log("HOOK ERROR BillingCycleSettings.updatePrefs(): " + ex.getMessage());
                            StackTraceElement[] elements = ex.getStackTrace();
                            XposedBridge.log("HOOK ERROR BillingCycleSettings.updatePrefs(): at " + elements[0].getClassName() + "." + elements[0].getMethodName() + "() line: " + elements[0].getLineNumber());
                            String dump=obj_dump(param.thisObject);
                            XposedBridge.log("HOOK ERROR BillingCycleSettings.updatePrefs() objDUMP: Dumping BillingCycleSettings ...");
                            for (String item : dump.split(System.getProperty("line.separator"))) {
                                XposedBridge.log("HOOK ERROR BillingCycleSettings.updatePrefs() objDUMP: " + item);
                            }
                            throw ex;
                        }

                    }
                });

            }
        }


    }

    /****************************
     Main Methods

     */

    //Compute methods
    //only for SDK<26
    private static long mComputeLastCycleBoundary(long currentTime, int cycleDay) {
        //Decode CycleDay
        Object[] decodedArr = decodeBitShiftedInt(cycleDay);
        Calendar pref_cycleDate = (Calendar) decodedArr[0];
        int pref_cycleDays = (int) decodedArr[1];

        //Debug - Wait... What is the request?
        if(DEBUG) { Format format = new SimpleDateFormat("dd/MM/yyyy");
                    XposedBridge.log("HOOK REQ LAST Cycle with prefTime:"+format.format(new Date(pref_cycleDate.getTimeInMillis()))+
                                    "*"+pref_cycleDays+"  for currentTime:"+format.format(new Date(currentTime))); }

        // Approach to date currentTime, when i am close choose Last <- or Next ->
        Calendar cycleDate = (Calendar) pref_cycleDate.clone();
        int m;
        if (cycleDate.getTimeInMillis()>=currentTime) m = -1; else m = 1;
        while ( daysBetween(cycleDate.getTimeInMillis(), currentTime) >= pref_cycleDays ) {
            cycleDate.add(Calendar.DAY_OF_YEAR, pref_cycleDays * m);
        }
        // Set Last Cycle
        if(cycleDate.getTimeInMillis()>=currentTime) cycleDate.add(Calendar.DAY_OF_YEAR, -pref_cycleDays);

        //Debug - Ok... That's my result.
        if(DEBUG) { Format format = new SimpleDateFormat("dd/MM/yyyy");
                    XposedBridge.log("HOOK       from prefTime:"+format.format(new Date(pref_cycleDate.getTimeInMillis()))+
                    " LAST to currentTime:"+format.format(new Date(currentTime))+
                    " is "+format.format(new Date(cycleDate.getTimeInMillis())) ); }

        //Return Last
        return cycleDate.getTimeInMillis();
    }

    private static long mComputeNextCycleBoundary(long currentTime, int cycleDay) {
        //Decode CycleDay
        Object[] decodedArr = decodeBitShiftedInt(cycleDay);
        Calendar pref_cycleDate = (Calendar) decodedArr[0];
        int pref_cycleDays = (int) decodedArr[1];

        //Debug - Wait... What is the request?
        if(DEBUG) { Format format = new SimpleDateFormat("dd/MM/yyyy");
                    XposedBridge.log("HOOK REQ NEXT Cycle with prefTime:"+format.format(new Date(pref_cycleDate.getTimeInMillis()))+
                    "*"+pref_cycleDays+"  for currentTime:"+format.format(new Date(currentTime))); }

        // Approach to date currentTime, when i am close choose Last <- or Next ->
        Calendar cycleDate = (Calendar) pref_cycleDate.clone();
        int m;
        if (cycleDate.getTimeInMillis()>currentTime) m = -1; else m = 1;
        while ( daysBetween(cycleDate.getTimeInMillis(), currentTime) >= pref_cycleDays ) {
            if(DEBUG) XposedBridge.log("HOOK               " + m + " " + pref_cycleDays );
            cycleDate.add(Calendar.DAY_OF_YEAR, pref_cycleDays * m);
        }
        // Set Next Cycle
        if(cycleDate.getTimeInMillis()<=currentTime) cycleDate.add(Calendar.DAY_OF_YEAR, pref_cycleDays);

        //Debug - Ok... That's my result.
        if(DEBUG) { Format format = new SimpleDateFormat("dd/MM/yyyy");
                    XposedBridge.log("HOOK       from prefTime:"+format.format(new Date(pref_cycleDate.getTimeInMillis()))+
                    " NEXT to currentTime:"+format.format(new Date(currentTime))+
                    " is "+format.format(new Date(cycleDate.getTimeInMillis())) ); }

        //Return Last
        return cycleDate.getTimeInMillis();
    }

    //Dialog methods
    private static Object createAdvDialog(XC_MethodHook.MethodHookParam param) {

        XposedBridge.log("HOOK createAdvDialog(): start!");

        DialogFragment mCycleEditorFragment = (DialogFragment) param.thisObject;                    //CycleEditorFragment
        final Context context = (Context) XposedHelpers.callMethod(param.thisObject, "getActivity");

        final Object target = XposedHelpers.callMethod(param.thisObject, "getTargetFragment");      //type sdk:23 DataUsageSummary sdk:24 BillingCycleSettings sdk:25 DataUsageEditController
        final Object editor;                                                                        //type NetworkPolicyEditor
        if (Build.VERSION.SDK_INT == 23) {
            editor = XposedHelpers.getObjectField(target, "mPolicyEditor");
        } else if (Build.VERSION.SDK_INT == 24) {
            Object services = XposedHelpers.getObjectField(target,"services");
            editor = XposedHelpers.getObjectField(services,"mPolicyEditor");
        } else if (Build.VERSION.SDK_INT == 25 || Build.VERSION.SDK_INT == 26 || Build.VERSION.SDK_INT == 27) {
            editor = XposedHelpers.callMethod(target, "getNetworkPolicyEditor");
        } else {
            XposedBridge.log("HOOK createAdvDialog(): SDK "+Build.VERSION.SDK_INT+" not supported!");
            return null;
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final LayoutInflater dialogInflater = LayoutInflater.from(builder.getContext());
        final View view = (View) XposedHelpers.callMethod(dialogInflater, "inflate", R_layout_data_usage_cycle_editor, null, false);

        if(DEBUG) XposedBridge.log("HOOK createAdvDialog(): R_id_cycle_day="+R_id_cycle_day);
        if(DEBUG) XposedBridge.log("HOOK createAdvDialog(): R_id_cycle_days="+R_id_cycle_days);
        if(DEBUG) XposedBridge.log("HOOK createAdvDialog(): R_id_datepicker="+R_id_datepicker);
        //view_dump(view);

        //get original cycleDayPicker (useless)
        final Object cycleDayPicker;
        if (Build.VERSION.SDK_INT == 23) {
            cycleDayPicker = view.findViewById(R_id_cycle_day);
        } else if (Build.VERSION.SDK_INT == 24 || Build.VERSION.SDK_INT == 25 || Build.VERSION.SDK_INT == 26 || Build.VERSION.SDK_INT == 27) {
            XposedHelpers.setObjectField(param.thisObject, "mCycleDayPicker", view.findViewById(R_id_cycle_day));
            cycleDayPicker = XposedHelpers.getObjectField(param.thisObject, "mCycleDayPicker");
        } else {
            Toast.makeText(context, "SDK "+Build.VERSION.SDK_INT+" not supported!", Toast.LENGTH_LONG).show();
            return null;
        }

        //Get cycleDay
        final Object args = XposedHelpers.callMethod(param.thisObject, "getArguments");
        final Object template = XposedHelpers.callMethod(args, "getParcelable", EXTRA_TEMPLATE );       //type NetworkTemplate
        final int cycleDay = (int) XposedHelpers.callMethod(editor, "getPolicyCycleDay", template);

        //Decode cycleDay
        Object[] decodedArr = decodeBitShiftedInt(cycleDay);
        Calendar pref_cycle_date = (Calendar) decodedArr[0];
        int pref_cycle_days = (int) decodedArr[1];

        //Update pref_cycle_date to Last Cycle
        while (pref_cycle_date.getTimeInMillis() < System.currentTimeMillis()) {     // Cycle until cycle_date > currentTime
            pref_cycle_date.add(Calendar.DAY_OF_MONTH, pref_cycle_days);
            if (DEBUG) {
                Format ft = new SimpleDateFormat("dd MMM yyyy");
                XposedBridge.log("HOOK createAdvDialog(): pref_cycle_date updated to " + ft.format(new Date(pref_cycle_date.getTimeInMillis())) + "");
            }
        }
        pref_cycle_date.add(Calendar.DAY_OF_MONTH, -pref_cycle_days);                //Set Last Cycle

        //Set layout cycleDayPicker (useless)
        try {
            if (DEBUG) XposedBridge.log("HOOK createAdvDialog(): cycleDayPicker id = " + XposedHelpers.callMethod(cycleDayPicker, "getId") + " " + cycleDayPicker.getClass().toString());
            XposedHelpers.callMethod(cycleDayPicker, "setMinValue", 1);
            XposedHelpers.callMethod(cycleDayPicker, "setMaxValue", 365);
            XposedHelpers.callMethod(cycleDayPicker, "setValue", cycleDay);
            XposedHelpers.callMethod(cycleDayPicker, "setWrapSelectorWheel", true);
        } catch (Exception ex) {
            XposedBridge.log("HOOK ERROR createAdvDialog(): Updating cycleDayPicker - StackTrace: " + ex.getMessage());
            StackTraceElement[] elements = ex.getStackTrace();
            XposedBridge.log("HOOK ERROR createAdvDialog(): at " + elements[0].getClassName() + "." + elements[0].getMethodName() + "() line: " + elements[0].getLineNumber());
            String dump=obj_dump(cycleDayPicker);
            XposedBridge.log("HOOK ERROR createAdvDialog(): objDUMP: Dumping cycleDayPicker ...");
            for (String item : dump.split(System.getProperty("line.separator"))) {
                XposedBridge.log("HOOK ERROR createAdvDialog(): objDUMP: " + item);
            }
        }

        //Set layout cycleDaysPicker
        final NumberPicker cycleDaysPicker = (NumberPicker) view.findViewById(R_id_cycle_days);
        String[] values = new String[100];
        for (int i = 0; i < 100; ++i) {
            values[i] = "" + (i + 1);
        }
        values[0] = "1 - " + context.getString(modR_strings_nr1_daily);
        values[6] = "7 - " + context.getString(modR_strings_nr7_weekly);
        values[29] = "30 - " + context.getString(modR_strings_nr30_fixedmonth);
        values[30] = "31 - " + context.getString(modR_strings_nr31_monthly);
        cycleDaysPicker.setDisplayedValues(values);
        cycleDaysPicker.setMinValue(1);
        cycleDaysPicker.setMaxValue(100);
        cycleDaysPicker.setValue(pref_cycle_days);
        cycleDaysPicker.setWrapSelectorWheel(true);
        cycleDaysPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        //Set layout cycleDatePicker
        final DatePicker cycleDatePicker = (DatePicker) view.findViewById(R_id_datepicker);
        int year = pref_cycle_date.get(Calendar.YEAR);
        int month = pref_cycle_date.get(Calendar.MONTH);
        int day = pref_cycle_date.get(Calendar.DAY_OF_MONTH);
        cycleDatePicker.updateDate(year, month, day);
        cycleDatePicker.setMaxDate(System.currentTimeMillis());
        cycleDatePicker.setDescendantFocusability(DatePicker.FOCUS_BLOCK_DESCENDANTS);

        // Set builder
        builder.setTitle("Advanced Cycle Editor");  //R.string.data_usage_cycle_editor_title
        builder.setView(view);
        if (Build.VERSION.SDK_INT == 23) {
            builder.setPositiveButton("OK",             //R.string.data_usage_cycle_editor_positive
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // clear focus to finish pending text edits
                            XposedHelpers.callMethod(cycleDayPicker, "clearFocus");
                            cycleDaysPicker.clearFocus();
                            cycleDatePicker.clearFocus();
                            int bs = encodeBitShiftedIntFromPickers(cycleDatePicker, cycleDaysPicker);

                            //Save in policy CycleDay
                            final String cycleTimezone = new Time().timezone;
                            XposedHelpers.callMethod(editor, "setPolicyCycleDay", template, bs, cycleTimezone);
                            XposedHelpers.callMethod(target, "updatePolicy", true);

                        }
                    });
        } else if (Build.VERSION.SDK_INT == 24 || Build.VERSION.SDK_INT == 25 || Build.VERSION.SDK_INT == 26 || Build.VERSION.SDK_INT == 27) {
            XposedHelpers.setAdditionalStaticField(param.thisObject,"mCycleDatePicker",cycleDatePicker);
            XposedHelpers.setAdditionalStaticField(param.thisObject,"mCycleDaysPicker",cycleDaysPicker);
            builder.setPositiveButton("OK", (DialogInterface.OnClickListener) param.thisObject);
        }

        //Create Dialog & return
        if (DEBUG) XposedBridge.log("HOOK onCreateDialog(): Completed!");
        return builder.create();
    }

    //Encoding methods
    private static int encodeBitShiftedIntFromPickers(DatePicker cycleDatePicker, NumberPicker cycleDaysPicker) {
        int bs = encodeBitShiftedInt(cycleDatePicker.getDayOfMonth(),cycleDatePicker.getMonth(),cycleDaysPicker.getValue());
        return bs;
    }
    private static int encodeBitShiftedInt(int dayOfMonth, int Month, int Days) {
        // Encode Day of Month, Month and Duration into one int
        // via BitShift method.
        int bs1 = dayOfMonth;
        int bs2 = Month;
        int bs3 = Days;
        int bs = (bs1 & 0xFF) | ((bs2 & 0xFF) << 8) | ((bs3 & 0xFF) << 16);
        if (DEBUG) XposedBridge.log("HOOK BITSH pref SAVED " + bs + " (" + bs1 + "." + bs2 + "." + bs3 + ")");
        return bs;
    }

    private static Object[] decodeBitShiftedInt(int cycleDay){

        // Get Preferences
        Calendar pref_cycleDate = Calendar.getInstance();

        //Set no hour
        pref_cycleDate.set(Calendar.HOUR_OF_DAY, 0);
        pref_cycleDate.set(Calendar.MINUTE, 0);
        pref_cycleDate.set(Calendar.SECOND, 0);
        pref_cycleDate.set(Calendar.MILLISECOND, 0);

        int pref_cycleDays = 31;
        if (DEBUG) XposedBridge.log("HOOK BITSH pref LOAD " + cycleDay + "");
        if(cycleDay <= 31){
            // Not Bitshifted
            pref_cycleDate.set(Calendar.DAY_OF_MONTH, cycleDay);
        } else {
            // Decode Day of Month, Month and Duration from one int
            // via BitShift method.
            try{
                int bs1 = cycleDay & 0xFF;          // Day of Month
                int bs2 = (cycleDay >> 8) & 0xFF;   // Month
                int bs3 = (cycleDay >> 16) & 0xFF;  // num Days
                if(DEBUG) XposedBridge.log("HOOK BITSH loaded bitshited Ints "+bs1+"."+bs2+"."+bs3+"");
                pref_cycleDate.set(Calendar.DAY_OF_MONTH, bs1);
                pref_cycleDate.set(Calendar.MONTH, bs2);
                if(pref_cycleDate.getTimeInMillis() > System.currentTimeMillis()) {
                    pref_cycleDate.set(Calendar.YEAR, pref_cycleDate.get(Calendar.YEAR) - 1);
                    if(DEBUG) XposedBridge.log("HOOK BITSH preference year set to "+pref_cycleDate.get(Calendar.YEAR)+"");
                }
                if(bs3 != 0) pref_cycleDays = bs3; else { pref_cycleDays = 31; XposedBridge.log("HOOK ERR pref_cycleDays=0 - forced 31!"); }
            } catch (Exception ex){
                XposedBridge.log("HOOK ERROR decodeBitShiftedInt(): " + ex.getMessage());
                StackTraceElement[] elements = ex.getStackTrace();
                XposedBridge.log("HOOK ERROR decodeBitShiftedInt(): at " + elements[0].getClassName() + "." + elements[0].getMethodName() + "() line: " + elements[0].getLineNumber());
            }
        }

        return new Object[] {pref_cycleDate, pref_cycleDays};
    }

    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    //Status Bar methods
    @TargetApi(26)
    private Object getAdvDataUsageInfo(XC_MethodHook.MethodHookParam param, Class<?> networkTemplate, Class<?> networkStatsHistory, Class<?> dataUsageInfo, Class<?> NetworkPolicyManager) {

        //Get Session
        Object session = XposedHelpers.callMethod(param.thisObject, "getSession");    //INetworkStatsSession
        if (session == null) {
            return XposedHelpers.callMethod(param.thisObject, "warn", "no stats session");
        }

        //Get Template
        Object template = null;
        if(Build.VERSION.SDK_INT == 21 || Build.VERSION.SDK_INT == 22 || Build.VERSION.SDK_INT == 23) {
            final Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
            final String subscriberId = (String) XposedHelpers.callMethod(param.thisObject, "getActiveSubscriberId", context);   //String
            if (subscriberId == null) {
                XposedHelpers.callMethod(param.thisObject, "warn", "no subscriber id");
            }
            if(Build.VERSION.SDK_INT != 21) {
                if (DEBUG) XposedBridge.log("HOOK UI getAdvDataUsageInfo(): subscriberId :" + subscriberId + "");
                Object mTelephonyManager = XposedHelpers.getObjectField(param.thisObject, "mTelephonyManager");
                template = XposedHelpers.callStaticMethod(networkTemplate, "buildTemplateMobileAll", subscriberId);
                template = XposedHelpers.callStaticMethod(networkTemplate, "normalize", template, XposedHelpers.callMethod(mTelephonyManager, "getMergedSubscriberIds"));
            }
        }
        else if(Build.VERSION.SDK_INT == 24 || Build.VERSION.SDK_INT == 25 || Build.VERSION.SDK_INT == 26 || Build.VERSION.SDK_INT == 27) {
            template = param.args[0];
        }

        final Object policy = XposedHelpers.callMethod(param.thisObject, "findNetworkPolicy", template);
        try {
            if(Build.VERSION.SDK_INT == 23) session = XposedHelpers.getObjectField(param.thisObject, "mSession");

            //Initialize vars
            int FIELD_RX_BYTES = XposedHelpers.getStaticIntField(networkStatsHistory, "FIELD_RX_BYTES");
            int FIELD_TX_BYTES = XposedHelpers.getStaticIntField(networkStatsHistory, "FIELD_TX_BYTES");
            int FIELDS = FIELD_RX_BYTES | FIELD_TX_BYTES;
            final Object history = XposedHelpers.callMethod(session, "getHistoryForNetwork", template, FIELDS); //type NetworkStatsHistory
            final long now = System.currentTimeMillis();
            final long start, end;

            //Compute Cycle boundaries
            if(Build.VERSION.SDK_INT == 26 || Build.VERSION.SDK_INT == 27) {
                if (policy != null) {
                    if (DEBUG) XposedBridge.log("HOOK UI getAdvDataUsageInfo(): policy NOT null");
                    Object iterator = XposedHelpers.callStaticMethod(NetworkPolicyManager, "cycleIterator", policy);
                    Object cycle = XposedHelpers.callMethod(iterator, "next");
                    ZonedDateTime cycle_start = (ZonedDateTime) XposedHelpers.getObjectField(cycle, "first");   //ZonedDateTime
                    if(cycle_start == null){
                        if (DEBUG) XposedBridge.log("HOOK UI getAdvDataUsageInfo(): iterator.next() is NULL!");
                        // period = last 4 wks
                        end = now;
                        start = now - DateUtils.WEEK_IN_MILLIS * 4;
                    } else {
                        if (DEBUG) XposedBridge.log("HOOK UI getAdvDataUsageInfo(): iterator.next() NOT NULL!");
                        start = cycle_start.toInstant().toEpochMilli();
                        ZonedDateTime cycle_end = (ZonedDateTime) XposedHelpers.getObjectField(cycle, "second");
                        end = cycle_end.toInstant().toEpochMilli();
                    }
                } else {
                    if (DEBUG) XposedBridge.log("HOOK UI getAdvDataUsageInfo(): policy is null");
                    // period = last 4 wks
                    end = now;
                    start = now - DateUtils.WEEK_IN_MILLIS * 4;
                }
            } else {
                int cycleDay = 0;
                if (policy != null) { cycleDay = (int) XposedHelpers.getObjectField(policy, "cycleDay"); }
                if (policy != null && cycleDay > 31) {
                    start = mComputeLastCycleBoundary(now, cycleDay);
                    end = mComputeNextCycleBoundary(now, cycleDay);
                } else {
                    // period = last 4 wks
                    end = now;
                    start = now - DateUtils.WEEK_IN_MILLIS * 4;
                }
            }


            //Formatting and finalize values
            final long callStart = System.currentTimeMillis();
            final Object entry = XposedHelpers.callMethod(history, "getValues", start, end, now, null);     //type NetworkStatsHistory.Entry
            final long callEnd = System.currentTimeMillis();
            if (DEBUG) XposedBridge.log("HOOK UI getAdvDataUsageInfo(): History call from " +
                    new Date(start) + " to " + new Date(end) + " now=" + new Date(now) + " took " + (callEnd - callStart) + ": " +
                    (String) XposedHelpers.callMethod(param.thisObject, "historyEntryToString", entry));
            if (entry == null) {
                if (DEBUG) XposedBridge.log("HOOK UI getAdvDataUsageInfo(): entry is null");
                return XposedHelpers.callMethod(param.thisObject, "warn", "no entry data");
            }
            final long totalBytes = XposedHelpers.getLongField(entry, "rxBytes") + XposedHelpers.getLongField(entry, "txBytes");

            //Create dataUsageInfo and return
            final Object usage = XposedHelpers.newInstance(dataUsageInfo);
            if(Build.VERSION.SDK_INT == 24 || Build.VERSION.SDK_INT == 25) { XposedHelpers.setLongField(usage, "startDate", (long) start); }
            XposedHelpers.setLongField(usage, "usageLevel", (long) totalBytes);
            XposedHelpers.setObjectField(usage, "period", (String) XposedHelpers.callMethod(param.thisObject, "formatDateRange", start, end));
            if (policy != null) {
                XposedHelpers.setLongField(usage, "limitLevel", XposedHelpers.getLongField(policy, "limitBytes") > 0 ? XposedHelpers.getLongField(policy, "limitBytes") : 0);
                XposedHelpers.setLongField(usage, "warningLevel", XposedHelpers.getLongField(policy, "warningBytes") > 0 ? XposedHelpers.getLongField(policy, "warningBytes") : 0);
            } else {
                XposedHelpers.setLongField(usage, "warningLevel", 2L * 1024 * 1024 * 1024);
            }
            Object mNetworkController = XposedHelpers.getObjectField(param.thisObject, "mNetworkController");
            if (usage != null && mNetworkController != null && Build.VERSION.SDK_INT != 21 ) {
                XposedHelpers.setObjectField(usage, "carrier", (String) XposedHelpers.callMethod(mNetworkController, "getMobileDataNetworkName"));
            }
            if (DEBUG && usage != null)XposedBridge.log("HOOK UI getAdvDataUsageInfo(): usageLevel=" + XposedHelpers.getObjectField(usage, "usageLevel") + " period=" + XposedHelpers.getObjectField(usage, "period") + "");

            return usage;
        } catch (Exception e) {
            if (DEBUG) XposedBridge.log("HOOK UI getAdvDataUsageInfo(): remote call failed StackTrace:" + Log.getStackTraceString(e));
            return XposedHelpers.callMethod(param.thisObject, "warn", "remote call failed");
        }

        //Return Null
        //if (DEBUG) XposedBridge.log("HOOK UI DataUsage: returning null!");
        //return null;
    }


    /****************************
     Utility Methods
     */

    public static void view_dump(View view){
        if(DEBUG) XposedBridge.log( "HOOK DUMP view " + view.toString());
        try {
            ViewGroup rootView = (ViewGroup) view.getRootView();
            int childViewCount = rootView.getChildCount();
            for (int i=0; i<childViewCount;i++){
                View workWithMe = rootView.getChildAt(i);
                if(DEBUG) XposedBridge.log( "HOOK DUMP view found {" + workWithMe.getId()+"} : "+ workWithMe.toString() + " " + workWithMe.getClass().getName().toString() + " " );
                //if(workWithMe instanceof LinearLayout ){
                    if(DEBUG) XposedBridge.log( "HOOK DUMP             " + workWithMe.getClass().getName().toString() + " contains:" );
                    ViewGroup llworkWithme = (ViewGroup) workWithMe;
                    int llchildViewCount = llworkWithme.getChildCount();
                    for (int lli=0; lli<llchildViewCount;lli++){
                        View llview = llworkWithme.getChildAt(lli);
                        if(DEBUG) XposedBridge.log( "HOOK DUMP              + {" + llview.getId()+"} : "+ llview.toString() + " " );
                    }
                //}
            }
        } catch (ClassCastException e){
            //Not a viewGroup here
            if(DEBUG) XposedBridge.log("HOOK DUMP view exception  Not a viewGroup here "+ e.toString() );
        } catch (NullPointerException e){
            //Root view is null
            if(DEBUG) XposedBridge.log("HOOK DUMP view exception  Root view is null "+ e.toString() );
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

    public static String obj_dump(Object obj) {
        StringBuilder result = new StringBuilder();
        String newLine = System.getProperty("line.separator");

        result.append( obj.getClass().getName() );
        result.append( " Object {" );
        result.append(newLine);

        try {
            //Fields
            Field[] fields = obj.getClass().getDeclaredFields();
            for ( Field field : fields  ) {
                result.append("  ");
                try {
                    result.append( " (" + field.getType().getName() + ") " + field.getName() );
                    try {
                        //requires access to private field:
                        result.append( " = " + field.get(obj) );
                    } catch ( Throwable ex ) {
                    }
                } catch ( Throwable ex ) {
                    result.append("Error dumping fields "+obj.getClass().getName()+":" + ex + "");
                }
                result.append(newLine);
            }
            //Methods
            /*Method[] methods = obj.getClass().getDeclaredMethods();
            for ( Method method : methods  ) {
                try {
                    result.append( method.getName() + " ");
                    Class retType = method.getReturnType();
                    Class[] paramTypes = method.getParameterTypes();
                    String name = method.getName();
                    result.append(" " + Modifier.toString(method.getModifiers()) + " " + retType.getName() + " " + name + "(");
                    for (int j = 0; j < paramTypes.length; j++) {
                        if (j > 0)
                            result.append(", ");
                        result.append(paramTypes[j].getName());
                    }
                    result.append(");");
                } catch ( Throwable ex ) {
                    XposedBridge.log("HOOK error dumping methods "+obj.getClass().getName()+":" + ex + "");
                }
                result.append(newLine);
            }*/
            //Inner Methods
            for (Class c = obj.getClass(); c != null; c = c.getSuperclass()) {
                for (Method method : c.getDeclaredMethods()) {
                    try {
                        if (c!=obj.getClass()) result.append( c.getName() + "$");
                        Class retType = method.getReturnType();
                        Class[] paramTypes = method.getParameterTypes();
                        String name = method.getName();
                        result.append(" " + Modifier.toString(method.getModifiers()) + " " + retType.getName() + " " + name + "(");
                        for (int j = 0; j < paramTypes.length; j++) {
                            if (j > 0)
                                result.append(", ");
                            result.append(paramTypes[j].getName());
                        }
                        result.append(");");
                    } catch ( Throwable ex ) {
                        result.append("Error dumping inner methods "+obj.getClass().getName()+":" + ex + "");
                    }
                    result.append(newLine);
                }
            }
        } catch (Throwable e) {
            result.append("Error dumping obj "+obj.getClass().getName()+":" + e + "");
        }

        result.append("}");

        return result.toString();
    }

}


