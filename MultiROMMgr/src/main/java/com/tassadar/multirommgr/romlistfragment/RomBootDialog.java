/*
 * This file is part of MultiROM Manager.
 *
 * MultiROM Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MultiROM Manager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MultiROM Manager. If not, see <http://www.gnu.org/licenses/>.
 */

package com.tassadar.multirommgr.romlistfragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.tassadar.multirommgr.Device;
import com.tassadar.multirommgr.Kernel;
import com.tassadar.multirommgr.MgrApp;
import com.tassadar.multirommgr.MultiROM;
import com.tassadar.multirommgr.R;
import com.tassadar.multirommgr.Rom;
import com.tassadar.multirommgr.SettingsFragment;
import com.tassadar.multirommgr.StatusAsyncTask;
import com.tassadar.multirommgr.Utils;

public class RomBootDialog extends DialogFragment implements View.OnClickListener {

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Rom rom = getArguments().getParcelable("rom");

        View v = inflater.inflate(R.layout.fragment_rom_boot, container, false);

        Button cancelBtn = (Button)v.findViewById(R.id.cancel_btn);
        cancelBtn.setOnClickListener(this);
        Button bootBtn = (Button)v.findViewById(R.id.boot_btn);
        bootBtn.setOnClickListener(this);

        TextView t = (TextView)v.findViewById(R.id.dialog_text);
        MultiROM m = StatusAsyncTask.instance().getMultiROM();
        if(m != null && !m.hasBootRomReqMultiROM()) {
            t.setText(Utils.getString(R.string.rom_boot_req_ver, MultiROM.MIN_BOOT_ROM_VER));
            bootBtn.setVisibility(View.GONE);
        } else {
            t.setText(Utils.getString(R.string.boot_rom, rom.name));
        }
        return v;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.cancel_btn:
            {
                Activity a = getActivity();
                if(a instanceof RomBootActivity)
                    a.finish();
                else
                    dismiss();
                break;
            }
            case R.id.boot_btn:
            {
                View v = getView();
                Rom rom = getArguments().getParcelable("rom");

                setCancelable(false);

                TextView t = (TextView)v.findViewById(R.id.dialog_text);
                t.setText(R.string.booting);

                Button b = (Button)v.findViewById(R.id.cancel_btn);
                b.setEnabled(false);
                b = (Button)v.findViewById(R.id.boot_btn);
                b.setEnabled(false);

                new Thread(new RomBootRunnable(rom)).start();
                break;
            }
        }
    }

    private class RomBootRunnable implements Runnable {
        private Rom m_rom;
        public RomBootRunnable(Rom rom) {
            m_rom = rom;
        }

        @Override
        public void run() {
            Activity a = getActivity();
            if(a == null)
                return;

            MultiROM m = StatusAsyncTask.instance().getMultiROM();
            boolean has_kexec = StatusAsyncTask.instance().hasKexecKernel();
            if(m == null) {
                m = new MultiROM();
                if(!m.findMultiROMDir() || !m.findVersion()) {
                    a.runOnUiThread(new SetErrorTextRunnable(R.string.rom_boot_failed));
                    return;
                }

                if(!m.hasBootRomReqMultiROM()) {
                    a.runOnUiThread(new SetErrorTextRunnable(R.string.rom_boot_req_ver,
                            MultiROM.MIN_BOOT_ROM_VER));
                    return;
                }

                SharedPreferences p = MgrApp.getPreferences();
                Device dev = Device.load(p.getString(SettingsFragment.DEV_DEVICE_NAME, Build.DEVICE));
                if(dev == null) {
                    a.runOnUiThread(new SetErrorTextRunnable(R.string.rom_boot_failed));
                    return;
                }

                Kernel k = new Kernel();
                has_kexec = k.findKexecHardboot(dev);
            }

            if(!has_kexec && m.isKexecNeededFor(m_rom)) {
                a.runOnUiThread(new SetErrorTextRunnable(R.string.rom_boot_kexec));
                return;
            }

            // this won't return unless it fails
            m.bootRom(m_rom);

            a.runOnUiThread(new SetErrorTextRunnable(R.string.rom_boot_failed));
        }
    }

    private class SetErrorTextRunnable implements Runnable {
        private String m_text;
        public SetErrorTextRunnable(int text_fmt, Object... args) {
            m_text = Utils.getString(text_fmt, args);
        }

        @Override
        public void run() {
            setCancelable(true);

            View v = getView();
            if(v != null) {
                TextView t = (TextView)v.findViewById(R.id.dialog_text);
                t.setText(m_text);

                Button b = (Button)v.findViewById(R.id.cancel_btn);
                b.setEnabled(true);
                b = (Button)v.findViewById(R.id.boot_btn);
                b.setVisibility(View.GONE);
            }
        }
    }
}
