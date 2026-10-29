package br.gov.ce.detran.treinamento.spring.util;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Locale;

/** Uma única imagem, independente de navegador, para a consulta e os relatórios. */
public final class PlacaImagem {
 private static final Font FONTE=fonte();
 private static final java.util.Map<Character,BufferedImage> GLIFOS=glifos();
 private static java.util.Map<Character,BufferedImage> glifos() {
  java.util.Map<Character,BufferedImage> mapa=new java.util.HashMap<>();
  for(char c:"ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray()) {
   try(InputStream in=PlacaImagem.class.getResourceAsStream("/fonts/fe-glyphs/"+c+".png")) {mapa.put(c,javax.imageio.ImageIO.read(in));}
   catch(Exception e){throw new IllegalStateException("Glifo FE ausente: "+c,e);}
  }
  return mapa;
 }
 private PlacaImagem() {}
 private static Font fonte() {
  try(InputStream in=PlacaImagem.class.getResourceAsStream("/fonts/Mandatory.otf")) {return Font.createFont(Font.TRUETYPE_FONT,in);}
  catch(Exception e) {throw new IllegalStateException("Fonte da placa não encontrada",e);}
 }
 public static BufferedImage renderizar(String valor,String tipo,String uf,String cidade) {
  String placa=valor==null?"":valor.replace("-","").trim().toUpperCase(Locale.ROOT);
  if(!placa.matches("[A-Z]{3}[0-9][A-Z0-9][0-9]{2}")) throw new IllegalArgumentException("Placa inválida");
  boolean mercosul=Character.isLetter(placa.charAt(4)),moto="MOTO".equals(tipo);
  int w=moto?200:mercosul?520:400,h=moto?170:mercosul?140:130;
  BufferedImage image=new BufferedImage(w*4,h*4,BufferedImage.TYPE_INT_ARGB);
  Graphics2D g=image.createGraphics();g.scale(4,4);
  g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
  g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
  g.setPaint(new GradientPaint(0,0,new Color(210,213,214),0,h,new Color(103,107,108)));
  g.fillRoundRect(0,0,w,h,32,32);
  g.setPaint(new GradientPaint(0,3,mercosul?Color.WHITE:new Color(218,220,216),0,h,mercosul?new Color(241,242,237):new Color(164,168,163)));
  g.fillRoundRect(2,2,w-4,h-4,28,28);
  g.setColor(new Color(58,60,58));g.setStroke(new BasicStroke(mercosul?1:2));g.drawRoundRect(4,4,w-8,h-8,24,24);
  int faixa=mercosul?(moto?25:28):(moto?24:25);
  if(mercosul) {
   g.setColor(new Color(0,48,149));g.fillRoundRect(3,3,w-6,faixa,26,26);g.fillRect(3,16,w-6,faixa-13);
   g.setColor(Color.WHITE);g.setFont(new Font("SansSerif",Font.BOLD,moto?12:18));centro(g,"BRASIL",w/2,faixa-6);
   g.setFont(new Font("SansSerif",Font.PLAIN,moto?4:6));g.drawString("MERCOSUL",moto?9:16,faixa-4);
   g.setFont(new Font("SansSerif",Font.BOLD,moto?9:13));g.drawString("✦",moto?14:28,moto?14:17);
   bandeira(g,w-(moto?34:49),7,moto?25:34,moto?15:20);
   g.setColor(Color.BLACK);g.setFont(new Font("SansSerif",Font.BOLD,moto?8:10));g.drawString("BR",moto?9:12,h-8);
   qr(g,moto?8:11,faixa+8,moto?15:20);
  } else {
   g.setColor(new Color(125,129,126));g.fillRoundRect(moto?10:42,8,w-(moto?20:84),18,3,3);
   g.setColor(new Color(205,207,202));g.fillRoundRect(moto?11:43,9,w-(moto?22:86),16,2,2);
   g.setColor(Color.BLACK);g.setFont(new Font("SansSerif",Font.BOLD,moto?9:12));
   String local=(uf==null?"UF":uf)+" - "+(cidade==null?"MUNICÍPIO":cidade);
   while(g.getFontMetrics().stringWidth(local)>w-(moto?30:100)) g.setFont(g.getFont().deriveFont(g.getFont().getSize2D()-0.5f));
   centro(g,local.toUpperCase(Locale.ROOT),w/2,21);
  }
  if(moto) {
   caracteres(g,placa.substring(0,3),mercosul?34:24,faixa+7,w-(mercosul?45:48),58,!mercosul);
   caracteres(g,placa.substring(3),14,faixa+69,w-28,61,!mercosul);
  } else caracteres(g,mercosul?placa:placa.substring(0,3)+"-"+placa.substring(3),mercosul?46:18,faixa+10,w-(mercosul?60:36),mercosul?94:73,!mercosul);
  int recuo=moto?40:mercosul?90:32;
  for(int x:new int[]{recuo,w-recuo}) {
   g.setPaint(new GradientPaint(x-3,4,Color.WHITE,x+3,10,new Color(100,103,103)));g.fillOval(x-3,5,6,6);
   g.setColor(new Color(82,82,82));g.setStroke(new BasicStroke(.7f));g.drawLine(x-1,6,x+1,10);
  }
  g.dispose();return image;
 }
 private static void caracteres(Graphics2D g,String texto,int x,int y,int w,int h,boolean relevo) {
  int celula=w/texto.length();
  for(int i=0;i<texto.length();i++) {
   String c=texto.substring(i,i+1);
   if(!relevo) {
    BufferedImage glifo=GLIFOS.get(c.charAt(0));
    int largura=(int)Math.min(celula*.83,h*.63);
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    g.drawImage(glifo,x+i*celula+(celula-largura)/2,y,largura,h,null);continue;
   }
   Font f=FONTE.deriveFont(100f);
   Shape forma=f.createGlyphVector(g.getFontRenderContext(),c).getOutline();Rectangle2D b=forma.getBounds2D();
   // Use a mesma escala horizontal para todos os glifos, preservando I e 1 estreitos.
   double larguraReferencia=f.createGlyphVector(g.getFontRenderContext(),"0").getOutline().getBounds2D().getWidth();
   double largura=Math.min(celula*.83,h*.63)*b.getWidth()/larguraReferencia;if(c.equals("-"))largura=celula*.55;
   AffineTransform t=new AffineTransform();t.translate(x+i*celula+(celula-largura)/2,y+(c.equals("-")?h*.48:0));t.scale(largura/b.getWidth(),(c.equals("-")?h*.12:h)/b.getHeight());t.translate(-b.getX(),-b.getY());Shape desenhada=t.createTransformedShape(forma);
   if(relevo) {g.translate(1,1);g.setColor(new Color(238,239,234));g.fill(desenhada);g.translate(-1,-1);}
   g.setColor(new Color(16,18,17));g.fill(desenhada);
  }
 }
 private static void centro(Graphics2D g,String s,int x,int y) {g.drawString(s,x-g.getFontMetrics().stringWidth(s)/2,y);}
 private static void bandeira(Graphics2D g,int x,int y,int w,int h) {
  g.setColor(new Color(0,151,57));g.fillRect(x,y,w,h);g.setColor(new Color(255,223,0));g.fillPolygon(new int[]{x+w/2,x+w-2,x+w/2,x+2},new int[]{y+2,y+h/2,y+h-2,y+h/2},4);
  g.setColor(new Color(0,39,118));g.fillOval(x+w/2-h/4,y+h/4,h/2,h/2);g.setColor(Color.WHITE);g.setStroke(new BasicStroke(.8f));g.drawLine(x+w/2-h/5,y+h/2-1,x+w/2+h/5,y+h/2+1);
 }
 private static void qr(Graphics2D g,int x,int y,int tamanho) {
  // Símbolo meramente ilustrativo: não contém assinatura nem URL de validação.
  double c=tamanho/21.0;g.setColor(Color.WHITE);g.fillRect(x-1,y-1,tamanho+2,tamanho+2);g.setColor(Color.BLACK);
  for(int a=0;a<21;a++)for(int b=0;b<21;b++) if((a*17+b*13+a*b)%7<3) g.fill(new Rectangle2D.Double(x+a*c,y+b*c,c,c));
  for(int[] p:new int[][]{{0,0},{14,0},{0,14}}) {g.setColor(Color.BLACK);g.fill(new Rectangle2D.Double(x+p[0]*c,y+p[1]*c,7*c,7*c));g.setColor(Color.WHITE);g.fill(new Rectangle2D.Double(x+(p[0]+1)*c,y+(p[1]+1)*c,5*c,5*c));g.setColor(Color.BLACK);g.fill(new Rectangle2D.Double(x+(p[0]+2)*c,y+(p[1]+2)*c,3*c,3*c));}
 }
}
