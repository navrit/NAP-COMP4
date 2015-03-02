import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.JTable;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.math.array.DoubleArray;
import static org.math.array.LinearAlgebra.eigen;
import static org.math.array.StatisticSample.*;
import static org.math.io.files.ASCIIFile.readDoubleArray;
import static org.math.io.parser.ArrayString.printDoubleArray;
import org.math.plot.Plot2DPanel;
import org.math.plot.Plot3DPanel;
import Jama.EigenvalueDecomposition;
import rcaller.RCaller;
import rcaller.RCaller.*;
import rcaller.RCode;



public
        class CSVHandling extends JFrame

{
    //////////////////////////////////////////////**GLOBAL VARIABLE DECLARATION*/
    //Main var
    final JFileChooser chooser = new JFileChooser();
    private File selectedFile = null;
    private String filePath = null;
    private double[][] readFile = null;
    private int fileLength = 0;
    private static String OS = null;
    //JTable var init
    private String[] cols = null;
    private Object[][] tableContents = null;
    private int columnCount = 0;
    //Column var init
    private double[] column0 = null;
    private double[] column1 = null;
    private double[] column2 = null;
    //Graphing
    public Plot2DPanel plot2 = new Plot2DPanel();
    public Plot3DPanel plot3 = new Plot3DPanel();
    //PCA
    private double[] meanX, stdevX = null;
    private double[][] Z = null; // X centered reduced
    private double[][] cov = null; // Z covariance matrix
    private double[][] U = null; // projection matrix
    private double[] info = null; // information matrix
    //Icon
    private Image icon = Toolkit.getDefaultToolkit()
                                .getImage(getClass()
                                .getResource("snorlax.png"));
    //////////////////////////////////////////////////////////END OF DECLARATION

    public
            CSVHandling() throws Exception
    {
        initComponents();
        initJChooser();
        initJTable();
        initPlots();
        doPCA();
        callR();
    }

    private
            static String getOsName()
    {
        if(OS == null) { OS = System.getProperty("os.name"); }
        return OS;
    }

   private
           static boolean isWindows()
   {
        return getOsName().startsWith("Windows");
   }

   private
           static boolean isLinux()
   {
        return getOsName().startsWith("Linux");
   }

   private
           static boolean isMac()
   {
        return getOsName().startsWith("Mac OS");
   }

    private
            void callR()
    {
        try
            {
                /**R session init*/
             RCaller rc = new RCaller();

                /**Correct OS dependant R binary - only Linux works like this*/
             if (isLinux())
                {rc.setRscriptExecutable("Rscript");}
             /*
             if (isMac())
                {rc.setRscriptExecutable("RMac");}

             if (isWindows())
                {rc.setRscriptExecutable("Rscript.exe");}
             */
             rc.cleanRCode();
             RCode code = new RCode();
             code.clear();

                /**R lib init*/
             code.addRCode("library(grid)");
             code.addRCode("library(lattice)");
             code.addRCode("library(hexbin)");
             code.addRCode("library(mlbench)");

                /**R --> Java var*/
             code.addDoubleArray("X", column0);
             code.addDoubleArray("Y", column1);
             code.addRCode("bin <- hexbin(X,Y,xbins=50)");

                /**R plot Hex bin*/
             File file = code.startPlot();
             code.addRCode("plot (bin, main=\"Hex Binning\")");
             code.endPlot();

                /**R plot smooth scatter*/
             File file1 = code.startPlot();
             code.addRCode("smoothScatter(X,Y)");
             code.endPlot();

                /**Send code to R interpreter*/
             rc.setRCode(code);
             rc.runOnly();

                /**Show plot*/
             code.showPlot(file);
             code.showPlot(file1);
            }
        catch(Exception e){}
    }

    private
            void playSound(String file)     // Exactly what it sounds like
    {                                       //  pun intended! ...
        try
            {
                Clip sound = AudioSystem.getClip();
                sound.open(AudioSystem.getAudioInputStream(
                           getClass().getResource(file)));
                sound.start();
                try
                {   //*Need to allow sounds to complete - all 1000ms*/
                    Thread.sleep(1000);
                }
                catch (Exception e) {}
            }
	catch (Exception e) { }
    }

    private
            void initJChooser()
    {
        boolean acceptable = false;
        chooser.setCurrentDirectory(new File("."));
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setDragEnabled(true);
        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.CANCEL_OPTION)
            {
                playSound("button-10");
                System.exit(0);
            }
        if (result == JFileChooser.ERROR_OPTION)
            {
                playSound("beep-5");
                System.exit(0);
                JOptionPane.showMessageDialog(null,
                                              "ERROR \n"
                                             +"Something fishy is going on...",
                                              "*Squak*",
                                              JOptionPane.ERROR_MESSAGE);
                System.out.println("ERROR");
            }

        while (acceptable == false)
        {
            selectedFile = chooser.getSelectedFile();
            filePath = selectedFile.getAbsolutePath();
            pathtextfield.setText(filePath);

        switch(result)
        {
        case JFileChooser.APPROVE_OPTION:
              {
                try
                  {
                   readFile = readDoubleArray(selectedFile);
                   fileLength = readFile.length;
                   acceptable = chooser.accept(selectedFile);
                   String str = printDoubleArray(readFile);
                   maintextarea.append(str);
                   playSound("button-2");
                  }
                catch(Exception e)
                  {
                      playSound("beep-5");
                    JOptionPane.showMessageDialog(null,
                                        "      I'm really sorry :(           \n"
                                       +"I really couldn't read your file    \n"
                                       +"Please forgive me and try again     \n"
                                       +"with a correctly formatted CSV file",
                                        "      *Squaaak*",
                                         JOptionPane.ERROR_MESSAGE);
                    JOptionPane.showMessageDialog(null,
                                        "The format which I accept requires: \n"
                                       +"   - No columns headings            \n"
                                       +"   - Any type of number (double)    \n"
                                       +"   - At least 2 columns of data   \n\n"
                                       +"eg.                                 \n"
                                       +"5,4.562346,-712.456                 \n"
                                       +"0.00012,127672149.4,-7123761273.823 \n"
                                       +"..." ,
                                        "    Please open a valid CSV file!",
                                         JOptionPane.INFORMATION_MESSAGE);
                    initJChooser();
                  }
              }
        break;

        case JFileChooser.CANCEL_OPTION:
            playSound("button-10");
            System.out.println("......Suicide......");
            System.exit(0);
        break;

        case JFileChooser.ERROR_OPTION:
            playSound("beep-5");
            JOptionPane.showMessageDialog(null,
                                        "ERROR \n"
                                       +"Something fishy is going on...",
                                        "*Squak*",
                                         JOptionPane.ERROR_MESSAGE);
            System.out.println("ERROR");
        break;
        }
        }

    }

    private
            void initJTable()
    {
        tableContents = new String[fileLength][readFile[0].length];

        for (int row = 0; row < fileLength; row++)
        {
            for (int col = 0; col < readFile[row].length; col++)
                   {
                    tableContents[row][col] = Double.toString(readFile[row][col]);
                    columnCount = col+1;
                   }
        }

        cols = new String [columnCount];

        for (int i = 0; i < columnCount; i++)
            {
            cols[i] = "Column "+(i+1);
            }

        table = new JTable(tableContents,cols);
        table.setColumnSelectionAllowed(true);
        jScrollPane2.setViewportView(table);
        table.getColumnModel()
             .getSelectionModel()
             .setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        int c1 = 0, c2 = 1, c3 = 2;

        column0 = new double[fileLength];
        column1 = new double[fileLength];
        column2 = new double[fileLength];

        for (int row = 0; row < fileLength; row++)
        {
            column0[row] = readFile[row][c1];
            try {
                 column1[row] = readFile[row][c2];
                }
            catch (Exception e0)
            {}

            try
                {
                 column2[row] = readFile[row][c3];
                }
            catch (Exception e1)
            {}
        }
    }

    private
            void initPlots()
    {
        plot2.addLinePlot("Line", column0, column1);
        plot2.addScatterPlot("Scatter", column0, column1);
        tabbedpane.setComponentAt(2, plot2);
        plot3.addScatterPlot("3D Scatter", column0, column1, column2);
        tabbedpane.setComponentAt(3, plot3);
    }

    private
            void doPCA()
    {
        PCA pca = new PCA();
        pca.print();
        pca.show();
        pca = null;                                                             /*Force the object "pca" to become eligible for garbage collection*/
    }

    public
            class PCA
    {

        public PCA()
        {
            stdevX = stddeviation(readFile);
            meanX = mean(readFile);
            Z = center_reduce(readFile);
            cov = covariance(Z);
            EigenvalueDecomposition e = eigen(cov);
            U = transpose((e.getV().getArray()));
            info = e.getRealEigenvalues(); // Covariance matrix is symmetric, so only real eigenvalues...
        }

        // Normalisation of x relatively to X mean and standard deviation
	private double[][]
                center_reduce(double[][] x)
        {
		double[][] y = new double[x.length][x[0].length];
		for (int i = 0; i < y.length; i++)
			for (int j = 0; j < y[i].length; j++)
				y[i][j] = (x[i][j] - meanX[j]) / stdevX[j];
		return y;
	}

        // De-normalisation of y relative to X mean and standard deviation
	public double[]
                inv_center_reduce(double[] y)
        {
            return inv_center_reduce(new double[][] { y })[0];
	}

        // De-normalisation of y relative to X mean and standard deviation
	public double[][] inv_center_reduce(double[][] y)
        {
            double[][] x = new double[y.length][y[0].length];
            for (int i = 0; i < x.length; i++)
                for (int j = 0; j < x[i].length; j++)
                    x[i][j] = (y[i][j] * stdevX[j]) + meanX[j];
            return x;
	}

	private void show()
        {
            Plot2DPanel pcaplot = new Plot2DPanel();

            // Initial Data plot
            pcaplot.addScatterPlot("Data", column0, column1);

            // Line plot of principle components
            pcaplot.addLinePlot(Math.rint(info[0] * 100 / sum(info)) + " %", meanX, inv_center_reduce(U[0]));
            pcaplot.addLinePlot(Math.rint(info[1] * 100 / sum(info)) + " %", meanX, inv_center_reduce(U[1]));

            // Display in JFrame   //new FrameView(pcaplot);
            tabbedpane.add("PCA Plot", pcaplot);
	}

	private void print()
        {
            // Display results
            term.append("PCA vector information \n\n");
            term.append("Projection vectors\n"
                         +DoubleArray.toString(transpose(U)));
            term.append("\n\n");
            term.append("Information about projection vector \n"
                         +DoubleArray.toString(info));
	}

    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jFrame1 = new javax.swing.JFrame();
        desktoppane = new javax.swing.JDesktopPane();
        internalframe = new javax.swing.JInternalFrame();
        tabbedpane = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        maintextarea = new javax.swing.JTextArea();
        pathtextfield = new javax.swing.JTextField();
        pathlabel = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        jPanel4 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jInternalFrame1 = new javax.swing.JInternalFrame();
        jScrollPane3 = new javax.swing.JScrollPane();
        term = new javax.swing.JTextArea();

        jFrame1.setTitle("N.A.P.");

        javax.swing.GroupLayout jFrame1Layout = new javax.swing.GroupLayout(jFrame1.getContentPane());
        jFrame1.getContentPane().setLayout(jFrame1Layout);
        jFrame1Layout.setHorizontalGroup(
            jFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        jFrame1Layout.setVerticalGroup(
            jFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("N.A.P. (Navrit's Astronomical Program)");
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setIconImage(icon);
        setMinimumSize(new java.awt.Dimension(750, 500));
        setName("");

        desktoppane.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        desktoppane.setDoubleBuffered(true);
        desktoppane.setMinimumSize(new java.awt.Dimension(300, 300));
        desktoppane.setName("NAP");

        internalframe.setIconifiable(true);
        internalframe.setMaximizable(true);
        internalframe.setResizable(true);
        internalframe.setTitle("Main frame");
        internalframe.setToolTipText("");
        internalframe.setMinimumSize(new java.awt.Dimension(60, 60));
        internalframe.setName("N.A.P.");
        internalframe.setVisible(true);

        tabbedpane.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        tabbedpane.setMinimumSize(new java.awt.Dimension(200, 200));
        tabbedpane.setName("");

        maintextarea.setBackground(new java.awt.Color(78, 147, 203));
        maintextarea.setColumns(20);
        maintextarea.setEditable(false);
        maintextarea.setFont(maintextarea.getFont());
        maintextarea.setForeground(new java.awt.Color(255, 255, 255));
        maintextarea.setRows(5);
        maintextarea.setToolTipText("String dump");
        maintextarea.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));
        maintextarea.setDragEnabled(true);
        jScrollPane1.setViewportView(maintextarea);

        pathtextfield.setEditable(false);
        pathtextfield.setText("  You moron!");
        pathtextfield.setToolTipText("The path of your input file");
        pathtextfield.setBorder(null);
        pathtextfield.setOpaque(false);
        pathtextfield.setRequestFocusEnabled(false);
        pathtextfield.setSelectionColor(new java.awt.Color(204, 255, 255));

        pathlabel.setText("Path :");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 447, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(pathlabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(pathtextfield)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(pathtextfield, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pathlabel))
                .addGap(22, 22, 22)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 269, Short.MAX_VALUE)
                .addContainerGap())
        );

        tabbedpane.addTab("String dump", jPanel1);

        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null}
            },
            new String [] {
                "RA", "DEC", "Z"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Double.class, java.lang.Double.class, java.lang.Double.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        table.setColumnSelectionAllowed(true);
        jScrollPane2.setViewportView(table);
        table.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 471, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 334, Short.MAX_VALUE)
        );

        tabbedpane.addTab("Table", jPanel2);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 471, Short.MAX_VALUE)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 334, Short.MAX_VALUE)
        );

        tabbedpane.addTab("2D Graph", jPanel4);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 471, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 334, Short.MAX_VALUE)
        );

        tabbedpane.addTab("3D graph", jPanel3);

        javax.swing.GroupLayout internalframeLayout = new javax.swing.GroupLayout(internalframe.getContentPane());
        internalframe.getContentPane().setLayout(internalframeLayout);
        internalframeLayout.setHorizontalGroup(
            internalframeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(tabbedpane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        internalframeLayout.setVerticalGroup(
            internalframeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(tabbedpane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        internalframe.setBounds(10, 40, 490, 400);
        desktoppane.add(internalframe, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jLabel1.setFont(jLabel1.getFont().deriveFont((jLabel1.getFont().getStyle() & ~java.awt.Font.ITALIC), 24));
        jLabel1.setForeground(new java.awt.Color(254, 254, 254));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("Navrit's Astronomical Program       (N.A.P.)");
        jLabel1.setFocusable(false);
        jLabel1.setBounds(10, 10, 510, 28);
        desktoppane.add(jLabel1, javax.swing.JLayeredPane.DEFAULT_LAYER);

        jInternalFrame1.setBackground(new java.awt.Color(0, 0, 0));
        jInternalFrame1.setClosable(true);
        jInternalFrame1.setForeground(new java.awt.Color(0, 255, 0));
        jInternalFrame1.setIconifiable(true);
        jInternalFrame1.setMaximizable(true);
        jInternalFrame1.setResizable(true);
        jInternalFrame1.setTitle("Diagnostic Terminal");
        jInternalFrame1.setVisible(true);

        term.setBackground(new java.awt.Color(0, 0, 0));
        term.setColumns(20);
        term.setEditable(false);
        term.setForeground(new java.awt.Color(0, 255, 0));
        term.setRows(5);
        term.setDragEnabled(true);
        jScrollPane3.setViewportView(term);

        javax.swing.GroupLayout jInternalFrame1Layout = new javax.swing.GroupLayout(jInternalFrame1.getContentPane());
        jInternalFrame1.getContentPane().setLayout(jInternalFrame1Layout);
        jInternalFrame1Layout.setHorizontalGroup(
            jInternalFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 509, Short.MAX_VALUE)
        );
        jInternalFrame1Layout.setVerticalGroup(
            jInternalFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 181, Short.MAX_VALUE)
        );

        jInternalFrame1.setBounds(340, 250, 520, 210);
        desktoppane.add(jInternalFrame1, javax.swing.JLayeredPane.DEFAULT_LAYER);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(desktoppane, javax.swing.GroupLayout.DEFAULT_SIZE, 632, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(desktoppane, javax.swing.GroupLayout.DEFAULT_SIZE, 471, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    
    public static
            void main(String args[])
    {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         */
        try
        {
            for (UIManager.LookAndFeelInfo info :
                    UIManager.getInstalledLookAndFeels())
            {
                if ("Nimbus".equals(info.getName()))
                {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex)
        {
            Logger.getLogger(CSVHandling.class.getName()).log(
                    Level.SEVERE, null, ex);
        }
        //</editor-fold>

        EventQueue.invokeLater(new Runnable()
        {
            public                              @Override
                   void run()
            {
                try
                    {
                     new CSVHandling().setVisible(true);
                    }
                catch (Exception e){}
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JDesktopPane desktoppane;
    private javax.swing.JInternalFrame internalframe;
    private javax.swing.JFrame jFrame1;
    private javax.swing.JInternalFrame jInternalFrame1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTextArea maintextarea;
    private javax.swing.JLabel pathlabel;
    private javax.swing.JTextField pathtextfield;
    private javax.swing.JTabbedPane tabbedpane;
    private javax.swing.JTable table;
    private javax.swing.JTextArea term;
    // End of variables declaration//GEN-END:variables
}