/*
 * Truefood coop.
 * 
 * Decompiled because it seems, the source was lost.
 *
 */

/*     */ package com.openbravo.pos.scale;
/*     */ 
/*     */ import com.openbravo.beans.JNumberDialog;
/*     */ import com.openbravo.data.gui.JMessageDialog;
/*     */ import com.openbravo.data.gui.MessageInf;
/*     */ import com.openbravo.pos.forms.AppLocal;
/*     */ import gnu.io.CommPortIdentifier;
/*     */ import gnu.io.NoSuchPortException;
/*     */ import gnu.io.PortInUseException;
/*     */ import gnu.io.SerialPort;
/*     */ import gnu.io.SerialPortEvent;
/*     */ import gnu.io.SerialPortEventListener;
/*     */ import gnu.io.UnsupportedCommOperationException;
/*     */ import java.awt.Component;
/*     */ import java.io.IOException;
/*     */ import java.io.InputStream;
/*     */ import java.io.OutputStream;
/*     */ import java.util.TooManyListenersException;
/*     */ import javax.swing.ImageIcon;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class ScaleOhausAviator7000
/*     */   implements Scale, SerialPortEventListener
/*     */ {
/*     */   private Component parent;
/*     */   private CommPortIdentifier m_PortIdPrinter;
/*     */   private SerialPort m_CommPortPrinter;
/*     */   private String m_sPortScale;
/*     */   private OutputStream m_out;
/*     */   private InputStream m_in;
/*     */   private boolean m_S_detected;
/*     */   private int m_S_error_detected;
/*     */   private static final int SCALE_READY = 0;
/*     */   private static final int SCALE_READING = 1;
/*     */   private static final int SCALE_READINGDECIMALS = 2;
/*     */   private double m_dWeightBuffer;
/*     */   private double m_dWeightDecimals;
/*     */   private int m_iStatusScale;
/*     */   
/*     */   public ScaleOhausAviator7000(Component parent, String sPortPrinter)
/*     */   {
/*  56 */     this.parent = parent;
/*  57 */     m_sPortScale = sPortPrinter;
/*  58 */     m_out = null;
/*  59 */     m_in = null;
/*     */     
/*  61 */     m_iStatusScale = SCALE_READY;
/*  62 */     m_dWeightBuffer = 0.0D;
/*  63 */     m_dWeightDecimals = 1.0D;
/*     */     
/*  65 */     m_S_detected = false;
/*  66 */     m_S_error_detected = 0;
/*     */   }
/*     */   
/*     */ 
/*     */   public Double readWeight()
/*     */   {
/*  72 */     synchronized (this)
/*     */     {
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*  85 */       this.m_iStatusScale = 0;
/*     */       
/*     */ 
/*  88 */       this.m_dWeightBuffer = 0.0D;
/*  89 */       this.m_dWeightDecimals = 1.0D;
/*  90 */       this.m_S_detected = false;
/*  91 */       this.m_S_error_detected = 0;
/*     */       
/*  93 */       write(new byte[] { 87 });
/*     */       
/*  95 */       write(new byte[] { 13 });
/*  96 */       flush();
/*     */       
/*     */ 
/*     */       try
/*     */       {
/* 101 */         wait(50L);
/*     */       }
/*     */       catch (InterruptedException e) {}
/*     */       
/*     */ 
/*     */ 
/*     */       try
/*     */       {
/* 109 */         while (this.m_in.available() > 0) {
/* 110 */           int b = this.m_in.read();
/*     */           
/*     */ 
/* 113 */           if (b == 10)
/*     */           {
/* 115 */             synchronized (this) {
/* 116 */               this.m_iStatusScale = 0;
/* 117 */               notifyAll();
/*     */             }
/* 119 */           } else if (((((b > 47) && (b < 58)) || (b == 46) ? 1 : 0) & (!this.m_S_detected ? 1 : 0)) != 0) {
/* 120 */             synchronized (this) {
/* 121 */               if (this.m_iStatusScale == 0) {
/* 122 */                 this.m_dWeightBuffer = 0.0D;
/* 123 */                 this.m_dWeightDecimals = 1.0D;
/* 124 */                 this.m_iStatusScale = 1;
/*     */               }
/* 126 */               if (b == 46) {
/* 127 */                 this.m_iStatusScale = 2;
/*     */               } else {
/* 129 */                 this.m_dWeightBuffer = (this.m_dWeightBuffer * 10.0D + b - 48.0D);
/* 130 */                 if (this.m_iStatusScale == 2) {
/* 131 */                   this.m_dWeightDecimals *= 10.0D;
/*     */                 }
/*     */               }
/*     */             }
/*     */           } else {
/* 136 */             if (b == 83) {
/* 137 */               this.m_S_detected = true;
/*     */             }
/*     */             
/* 140 */             if ((this.m_S_detected) && 
/* 141 */               (b == 50)) {
/* 142 */               this.m_S_error_detected += 1;
/*     */             }
/*     */           }
/*     */         }
/*     */       }
/*     */       catch (IOException eIO) {}
/*     */     }
/*     */     
/*     */ 
/*     */ 
/*     */ 
/* 153 */     if (this.m_S_error_detected > 0) {
/* 154 */       this.m_iStatusScale = 0;
/* 155 */       this.m_dWeightBuffer = 0.0D;
/* 156 */       this.m_dWeightDecimals = 1.0D;
/*     */       
/*     */ 
/* 159 */       return JNumberDialog.showEditNumber(this.parent, AppLocal.getIntString("label.scale"), AppLocal.getIntString("label.scaleinput"), new ImageIcon(ScaleDialog.class.getResource("/com/openbravo/images/ark2.png")));
/*     */     }
/*     */     
/* 162 */     if (this.m_iStatusScale == 0)
/*     */     {
/* 164 */       double dWeight = this.m_dWeightBuffer / this.m_dWeightDecimals;
/* 165 */       this.m_dWeightBuffer = 0.0D;
/* 166 */       this.m_dWeightDecimals = 1.0D;
/* 167 */       return new Double(dWeight);
/*     */     }
/* 169 */     this.m_iStatusScale = 0;
/* 170 */     this.m_dWeightBuffer = 0.0D;
/* 171 */     this.m_dWeightDecimals = 1.0D;
/*     */     
/*     */ 
/*     */ 
/* 175 */     return JNumberDialog.showEditNumber(this.parent, AppLocal.getIntString("label.scale"), AppLocal.getIntString("label.scaleinput"), new ImageIcon(ScaleDialog.class.getResource("/com/openbravo/images/ark2.png")));
/*     */   }
/*     */   
/*     */ 
/*     */   private void flush()
/*     */   {
/*     */     try
/*     */     {
/* 183 */       this.m_out.flush();
/*     */     }
/*     */     catch (IOException e) {}
/*     */   }
/*     */   
/*     */   private void write(byte[] data) {
/*     */     try {
/* 190 */       if (this.m_out == null) {
/* 191 */         this.m_PortIdPrinter = CommPortIdentifier.getPortIdentifier(this.m_sPortScale);
/*     */         
/* 193 */         this.m_CommPortPrinter = ((SerialPort)this.m_PortIdPrinter.open("PORTID", 2000));
/*     */         
/* 195 */         this.m_out = this.m_CommPortPrinter.getOutputStream();
/* 196 */         this.m_in = this.m_CommPortPrinter.getInputStream();
/* 197 */         this.m_CommPortPrinter.addEventListener(this);
/* 198 */         this.m_CommPortPrinter.notifyOnDataAvailable(true);
/* 199 */         this.m_CommPortPrinter.setSerialPortParams(9600, 8, 1, 0);
/*     */       }
/*     */       
/* 202 */       this.m_out.write(data);
/*     */     } catch (NoSuchPortException e) {
/* 204 */       e.printStackTrace();
/*     */     } catch (PortInUseException e) {
/* 206 */       JMessageDialog.showMessage(this.parent, new MessageInf(-33554432, "COM port already in use, close any other applications that are using: ".concat(this.m_sPortScale), null));
/* 207 */       e.printStackTrace();
/*     */     } catch (UnsupportedCommOperationException e) {
/* 209 */       e.printStackTrace();
/*     */     } catch (TooManyListenersException e) {
/* 211 */       e.printStackTrace();
/*     */     } catch (IOException e) {
/* 213 */       e.printStackTrace();
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */   public void serialEvent(SerialPortEvent e)
/*     */   {
/* 220 */     switch (e.getEventType()) {
/*     */     case SerialPortEvent.DATA_AVAILABLE: 
/* 232 */       JMessageDialog.showMessage(this.parent, new MessageInf(-33554432, "Data available interrupt received", null));
                    break;
                default:
                    break;
/*     */     }
/*     */   }
/*     */ }
