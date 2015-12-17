/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.openbravo.pos.scale;

/**
 * Setting the scales to NCI Protocol, I have proven communications (bi-directionally) using HyperTerminal to send applicable NCI commands through COM1, and receiving weight & scales-status data back from the scales:

RS232 Comms:        9600 bps       8 data, no parity, 1 stop bit

Scales protocol:      (NCI)            W<CR>     = send weight & status

Rcv data format:     (NCI)            00.000KG<LF>Snn

nn=00 no error;  nn=20 zero/tare weight error;  nn=02 overweight error (12kg max)
 *
 * @author R. Shute 06/08/12
 */
 
import gnu.io.*;
import java.io.*;
import java.util.TooManyListenersException;
import com.openbravo.data.gui.JMessageDialog;
import com.openbravo.data.gui.MessageInf;
import java.awt.Component;
import com.openbravo.beans.JNumberDialog;
import com.openbravo.pos.forms.AppLocal;
import javax.swing.ImageIcon;

public class ScaleOhausAviator7000 implements Scale, SerialPortEventListener {

    private Component parent;

    private CommPortIdentifier m_PortIdPrinter;
    private SerialPort m_CommPortPrinter;

    private String m_sPortScale;
    private OutputStream m_out;
    private InputStream m_in;

    private boolean m_S_detected;
    private int m_S_error_detected;

    private static final int SCALE_READY = 0;
    private static final int SCALE_READING = 1;
    private static final int SCALE_READINGDECIMALS = 2;

    private double m_dWeightBuffer;
    private double m_dWeightDecimals;
    private int m_iStatusScale;

    /** Creates a new instance of ScaleComm */
    public ScaleOhausAviator7000(Component parent, String sPortPrinter) {
        this.parent = parent;
        m_sPortScale = sPortPrinter;
        m_out = null;
        m_in = null;

        m_iStatusScale = SCALE_READY;
        m_dWeightBuffer = 0.0;
        m_dWeightDecimals = 1.0;

        m_S_detected = false;
        m_S_error_detected = 0;

    }

    public Double readWeight() {

        synchronized(this) {
        //JMessageDialog.showMessage(this.parent, new MessageInf(MessageInf.SGN_WARNING, "readWeight",null ));

            /*if (m_iStatusScale != SCALE_READY) {
                try {
                    wait(1000);
                } catch (InterruptedException e) {
                }
                if (m_iStatusScale != SCALE_READY) {
                    // bascula tonta.
                    m_iStatusScale = SCALE_READY;
                }
            }*/
            m_iStatusScale = SCALE_READY;

            // initialise
            m_dWeightBuffer = 0.0;
            m_dWeightDecimals = 1.0;
            m_S_detected = false;
            m_S_error_detected = 0;

            write(new byte[] {0x57}); // W, this get to 3 decimal places
            //write(new byte[] {0x48}); // H, this get to 4 decimal places
            write(new byte[] {0x0D}); // CR
            flush();
            //JMessageDialog.showMessage(this.parent, new MessageInf(MessageInf.SGN_WARNING, "sentReadCommand",null ));

            try {

                wait(50);
            } catch (InterruptedException e) {
            }


            try {
                  //JMessageDialog.showMessage(this.parent, new MessageInf(MessageInf.SGN_WARNING, "in try ",null ));
                    
                    while (m_in.available() > 0/*&(b != 0x004B*/) {//End while loop when K detected
                        int b = m_in.read();
                        //JMessageDialog.showMessage(this.parent, new MessageInf(MessageInf.SGN_WARNING, "readWeight ".concat(Integer.toHexString(b)),null ));

                        if (b == 0x000A) { // LF ASCII
                            // Fin de lectura
                            synchronized (this) {
                                m_iStatusScale = SCALE_READY;
                                notifyAll();
                            }
                        } else if (((b > 0x002F && b < 0x003A) || b == 0x002E)&(!m_S_detected)){ //0-9 or .
                            synchronized(this) {
                                if (m_iStatusScale == SCALE_READY) {
                                    m_dWeightBuffer = 0.0; // se supone que esto debe estar ya garantizado
                                    m_dWeightDecimals = 1.0;
                                    m_iStatusScale = SCALE_READING;
                                }
                                if (b == 0x002E) { // .
                                    m_iStatusScale = SCALE_READINGDECIMALS;
                                } else {
                                    m_dWeightBuffer = m_dWeightBuffer * 10.0 + b - 0x0030;
                                    if (m_iStatusScale == SCALE_READINGDECIMALS) {
                                        m_dWeightDecimals *= 10.0;
                                    }
                                }
                            }
                        } else {//This will be called when any other characters appear
                            if (b == 0x0053){// S
                                m_S_detected = true;
                            }

                            if (m_S_detected){
                              if (b == 0x0032){ //See coments at top for error cosed
                                  m_S_error_detected++;
                              }

                            }
                        }
                        //JMessageDialog.showMessage(this.parent, new MessageInf(MessageInf.SGN_WARNING, "weight buffer ".concat(Double.toString(m_dWeightBuffer)),null ));
                    }

                } catch (IOException eIO) {}
        }

            if (m_S_error_detected > 0){//Check if errors were detected.
                 m_iStatusScale = SCALE_READY;
                 m_dWeightBuffer = 0.0;
                 m_dWeightDecimals = 1.0;

                 // If weight is Erroneous then pop up a number box for the weight to be entered manually.
                 return JNumberDialog.showEditNumber(parent, AppLocal.getIntString("label.scale"), AppLocal.getIntString("label.scaleinput"), new ImageIcon(ScaleDialog.class.getResource("/com/openbravo/images/ark2.png")));
                 //return new Double(0.0);
            }else {
            if (m_iStatusScale == SCALE_READY) {
                 // hemos recibido cositas o si no hemos recibido nada estamos a 0.0
                 double dWeight = m_dWeightBuffer / m_dWeightDecimals;
                 m_dWeightBuffer = 0.0;
                 m_dWeightDecimals = 1.0;
                 return new Double(dWeight);
                } else {
                 m_iStatusScale = SCALE_READY;
                 m_dWeightBuffer = 0.0;
                 m_dWeightDecimals = 1.0;

                 // If weight is 0 then pop up a number box for the weight to be entered manually.
                 // Set title for grams Kilos, ounzes, pounds, ...
                return JNumberDialog.showEditNumber(parent, AppLocal.getIntString("label.scale"), AppLocal.getIntString("label.scaleinput"), new ImageIcon(ScaleDialog.class.getResource("/com/openbravo/images/ark2.png")));
                //return new Double(0.0);
                }
            }
    }

    private void flush() {
        try {
            m_out.flush();
        } catch (IOException e) {
        }
    }

    private void write(byte[] data) {
        try {
            if (m_out == null) {
                m_PortIdPrinter = CommPortIdentifier.getPortIdentifier(m_sPortScale); // Tomamos el puerto

                m_CommPortPrinter = (SerialPort) m_PortIdPrinter.open("PORTID", 2000); // Abrimos el puerto

                m_out = m_CommPortPrinter.getOutputStream(); // Tomamos el chorro de escritura
                m_in = m_CommPortPrinter.getInputStream();
                m_CommPortPrinter.addEventListener(this);
                m_CommPortPrinter.notifyOnDataAvailable(true);
                m_CommPortPrinter.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE); // Configuramos el puerto
            }
               //JMessageDialog.showMessage(this.parent, new MessageInf(MessageInf.SGN_WARNING, "about to write Data",null ));
            m_out.write(data);
        } catch (NoSuchPortException e) {
            e.printStackTrace();
        } catch (PortInUseException e) {
            JMessageDialog.showMessage(this.parent, new MessageInf(MessageInf.SGN_WARNING, "COM port already in use, close any other applications that are using: ".concat(m_sPortScale),null ));
            e.printStackTrace();
        } catch (UnsupportedCommOperationException e) {
            e.printStackTrace();
        } catch (TooManyListenersException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void serialEvent(SerialPortEvent e) {

	// Determine type of event.
	switch (e.getEventType()) {
            case SerialPortEvent.BI:
            case SerialPortEvent.OE:
            case SerialPortEvent.FE:
            case SerialPortEvent.PE:
            case SerialPortEvent.CD:
            case SerialPortEvent.CTS:
            case SerialPortEvent.DSR:
            case SerialPortEvent.RI:
            case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
                break;
            case SerialPortEvent.DATA_AVAILABLE:
               JMessageDialog.showMessage(this.parent, new MessageInf(MessageInf.SGN_WARNING, "Data available interrupt received",null ));

        }
    }
}
