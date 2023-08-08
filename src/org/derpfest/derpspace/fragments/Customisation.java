/*
 * Copyright (C) 2018 AospExtended ROM Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.derpfest.derpspace.fragments;

import static android.os.UserHandle.USER_CURRENT;
import static android.os.UserHandle.USER_SYSTEM;

import android.app.ActivityManagerNative;
import android.app.UiModeManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.IWindowManager;
import android.view.View;
import android.view.WindowManagerGlobal;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.util.derp.derpUtils;
import com.android.internal.util.derp.ThemeUtils;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;

import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.derpfest.support.preferences.SystemSettingListPreference;

@SearchIndexable
public class Customisation extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String TAG = "Customisation";
    private static final String KEY_QS_PANEL_STYLE  = "qs_panel_style";
    private static final String VOLUMEBAR_STYLES = "VOLUME_BAR_STYLES";

    private static final String VOLUMEBAR_OVERLAY_STYLE1 = "com.custom.overlay.systemui.volume1";
    private static final String VOLUMEBAR_OVERLAY_STYLE2 = "com.custom.overlay.systemui.volume2"; 
    private static final String VOLUMEBAR_OVERLAY_STYLE3 = "com.custom.overlay.systemui.volume3";        
    private static final String VOLUMEBAR_OVERLAY_STYLE4 = "com.custom.overlay.systemui.volume4"; 
    private static final String VOLUMEBAR_OVERLAY_STYLE5 = "com.custom.overlay.systemui.volume5";

    private static final String KEY_QS_UI_STYLE  = "qs_ui_style";
    private static final String overlayThemeTarget  = "com.android.systemui";

    private SystemSettingListPreference CustomVolumeStyle;

    private Context mContext;
    private Handler mHandler;
    private IOverlayManager mOverlayManager;
    private IOverlayManager mOverlayService;
    private SystemSettingListPreference mQsStyle;
    private ThemeUtils mThemeUtils;
    private SystemSettingListPreference mQsUI;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.customisation);

        final ContentResolver resolver = getActivity().getContentResolver();
        final PreferenceScreen screen = getPreferenceScreen();

        mThemeUtils = new ThemeUtils(getActivity());

        mOverlayService = IOverlayManager.Stub
        .asInterface(ServiceManager.getService(Context.OVERLAY_SERVICE));

        mQsStyle = (SystemSettingListPreference) findPreference(KEY_QS_PANEL_STYLE);
	mQsUI = (SystemSettingListPreference) findPreference(KEY_QS_UI_STYLE);
        mCustomSettingsObserver.observe();

        mContext = getActivity();

        mOverlayService = IOverlayManager.Stub
                .asInterface(ServiceManager.getService(Context.OVERLAY_SERVICE));

        CustomVolumeStyle = (SystemSettingListPreference) findPreference("VOLUME_BAR_STYLES");
        int CuVolumeStyle = Settings.System.getIntForUser(getContentResolver(),
                "VOLUME_BAR_STYLES", 0, UserHandle.USER_CURRENT);
        int valueIndexvol = CustomVolumeStyle.findIndexOfValue(String.valueOf(CuVolumeStyle));
        CustomVolumeStyle.setValueIndex(valueIndexvol >= 0 ? valueIndexvol : 0);
        CustomVolumeStyle.setSummary(CustomVolumeStyle.getEntry());
        CustomVolumeStyle.setOnPreferenceChangeListener(this);

        boolean udfpsResPkgInstalled = derpUtils.isPackageInstalled(getContext(),
                "org.derp.udfps.resources");
        PreferenceCategory udfps = (PreferenceCategory) screen.findPreference("udfps_category");
        if (!udfpsResPkgInstalled) {
            screen.removePreference(udfps);
        }
    }

    private CustomSettingsObserver mCustomSettingsObserver = new CustomSettingsObserver(mHandler);
    private class CustomSettingsObserver extends ContentObserver {

        CustomSettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            Context mContext = getContext();
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.QS_PANEL_STYLE),
                    false, this, UserHandle.USER_ALL);
	    resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.QS_UI_STYLE),
	            false, this, UserHandle.USER_ALL);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
        if (uri.equals(Settings.System.getUriFor(Settings.System.QS_PANEL_STYLE)) || uri.equals(Settings.System.getUriFor(Settings.System.QS_UI_STYLE))) {
		updateQsStyle();
            }
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.DERP;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mQsStyle) {
           mCustomSettingsObserver.observe();
           return true;
	} else if (preference == mQsStyle || preference == mQsUI) {
	mCustomSettingsObserver.observe();
	  return true;
       } else if (preference == CustomVolumeStyle) {
            int VolumeStyle = Integer.valueOf((String) newValue);
            Settings.System.putIntForUser(getContentResolver(),
                    "VOLUME_BAR_STYLES", VolumeStyle, UserHandle.USER_CURRENT);
            CustomVolumeStyle.setSummary(CustomVolumeStyle.getEntries()[VolumeStyle]);
                if (VolumeStyle == 0) {
                   try {
                      mOverlayService.setEnabled(VOLUMEBAR_OVERLAY_STYLE1, false, USER_CURRENT);
                      mOverlayService.setEnabled(VOLUMEBAR_OVERLAY_STYLE2, false, USER_CURRENT);
                      mOverlayService.setEnabled(VOLUMEBAR_OVERLAY_STYLE3, false, USER_CURRENT);
                      mOverlayService.setEnabled(VOLUMEBAR_OVERLAY_STYLE4, false, USER_CURRENT);
                      mOverlayService.setEnabled(VOLUMEBAR_OVERLAY_STYLE5, false, USER_CURRENT);     
                   } catch (RemoteException re) {
                      throw re.rethrowFromSystemServer();
                   }
               } else if (VolumeStyle == 1) {
                   try {
                      mOverlayService.setEnabledExclusiveInCategory(VOLUMEBAR_OVERLAY_STYLE1, USER_CURRENT);   
                   } catch (RemoteException re) {
                      throw re.rethrowFromSystemServer();
                   }
               } else if (VolumeStyle == 2) {
                   try {
                      mOverlayService.setEnabledExclusiveInCategory(VOLUMEBAR_OVERLAY_STYLE2, USER_CURRENT);   
                   } catch (RemoteException re) {
                      throw re.rethrowFromSystemServer();
                   }
                } else if (VolumeStyle == 3) {
                   try {
                      mOverlayService.setEnabledExclusiveInCategory(VOLUMEBAR_OVERLAY_STYLE3, USER_CURRENT);     
                   } catch (RemoteException re) {
                      throw re.rethrowFromSystemServer();
                   }
                } else if (VolumeStyle == 4) {
                   try {
                      mOverlayService.setEnabledExclusiveInCategory(VOLUMEBAR_OVERLAY_STYLE4, USER_CURRENT);     
                   } catch (RemoteException re) {
                      throw re.rethrowFromSystemServer();
                   }
                } else if (VolumeStyle == 5) {
                   try {
                      mOverlayService.setEnabledExclusiveInCategory(VOLUMEBAR_OVERLAY_STYLE5, USER_CURRENT);     
                   } catch (RemoteException re) {
                      throw re.rethrowFromSystemServer();
                   }
                }
            return true;
          }
        return false;
    }

    private void updateQsStyle() {
        ContentResolver resolver = getActivity().getContentResolver();

        boolean isA11Style = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.QS_UI_STYLE , 1, UserHandle.USER_CURRENT) == 1;

        int qsPanelStyle = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.QS_PANEL_STYLE , 0, UserHandle.USER_CURRENT);

	String qsUIStyleCategory = "android.theme.customization.qs_ui";
	String qsPanelStyleCategory = "android.theme.customization.qs_panel";

	/// reset all overlays before applying
	resetQsOverlays(qsPanelStyleCategory);
	resetQsOverlays(qsUIStyleCategory);

	if (isA11Style) {
	    setQsStyle("com.android.system.qs.ui.A11", qsUIStyleCategory);
	}

	if (qsPanelStyle == 0) return;

        switch (qsPanelStyle) {
            case 1:
              setQsStyle("com.android.system.qs.outline", qsPanelStyleCategory);
              break;
            case 2:
            case 3:
              setQsStyle("com.android.system.qs.twotoneaccent", qsPanelStyleCategory);
              break;
            case 4:
              setQsStyle("com.android.system.qs.shaded", qsPanelStyleCategory);
              break;
            case 5:
              setQsStyle("com.android.system.qs.cyberpunk", qsPanelStyleCategory);
              break;
            case 6:
              setQsStyle("com.android.system.qs.neumorph", qsPanelStyleCategory);
              break;
            case 7:
              setQsStyle("com.android.system.qs.reflected", qsPanelStyleCategory);
              break;
            case 8:
              setQsStyle("com.android.system.qs.surround", qsPanelStyleCategory);
              break;
            case 9:
              setQsStyle("com.android.system.qs.thin", qsPanelStyleCategory);
              break;
            case 10:
              setQsStyle("com.android.system.qs.twotoneaccenttrans", qsPanelStyleCategory);
              break;
            default:
              break;
        }
    }

    public void resetQsOverlays(String category) {
        mThemeUtils.setOverlayEnabled(category, overlayThemeTarget, overlayThemeTarget);
    }

    public void setQsStyle(String overlayName, String category) {
        mThemeUtils.setOverlayEnabled(category, overlayName, overlayThemeTarget);
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                    boolean enabled) {
                final ArrayList<SearchIndexableResource> result = new ArrayList<>();
                final SearchIndexableResource sir = new SearchIndexableResource(context);
                sir.xmlResId = R.xml.customisation;
                result.add(sir);
                return result;
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                final List<String> keys = super.getNonIndexableKeys(context);
                return keys;
            }
    };
}
