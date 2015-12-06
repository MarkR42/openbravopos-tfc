/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.openbravo.pos.printer.escpos;
import com.openbravo.pos.printer.DeviceTicket;
/**
 *
 * @author j
 */
public class DeviceDisplayBA60 extends DeviceDisplaySerial {

private UnicodeTranslator trans;
    private String l1;
    private String l2;
    private String oldl1;
    private String oldl2;

    public DeviceDisplayBA60(PrinterWritter display, UnicodeTranslator trans) {
        this.trans = trans;
        init(display);
    }

    @Override
    public void initVisor() {
        display.write(new byte[]{0x1B, 0x5B, 0x32, 0x4A}); // Clear Display
        display.write(new byte[]{0x1B, 0x5B, 0x31, 0x3B, 0x31, 0x48, }); // Home the cursor
        display.flush();
        l1="";
        l2="";
        oldl1="";
        oldl2="";
    }

    public void repaintLines() {
        l1=m_displaylines.getLine1();
        l2=m_displaylines.getLine2();
        if (!l1.equals(oldl1)) {
          display.write(new byte[]{0x1B, 0x5B, 0x31, 0x3B, 0x31, 0x48, }); // Home the cusor
          display.write(trans.transString(DeviceTicket.alignLeft(m_displaylines.getLine1(), 20)));
          oldl1=l1;
        }
        if (!l2.equals(oldl2)) {
          display.write(new byte[]{0x1B, 0x5B, 0x32, 0x3B, 0x31, 0x48, }); // Move cursor to 2nd line
          display.write(trans.transString(DeviceTicket.alignLeft(m_displaylines.getLine2(), 20)));
          oldl2=l2;
        }
        display.flush();
    }
}
