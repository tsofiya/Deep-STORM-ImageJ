/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */
import UI.viewNetwork;
import net.imagej.*;

import java.util.ArrayList;

import net.imagej.tensorflow.TensorFlowService;
import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.modelimport.keras.KerasModelImport;
import org.ejml.simple.SimpleMatrix;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;


import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import com.opencsv.CSVReader;

import java.awt.image.BufferedImage;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.scijava.io.location.FileLocation;
import org.tensorflow.SavedModelBundle;


/**
 * A very simple plugin.
 * <p>
 * The annotation {@code @Plugin} lets ImageJ know that this is a plugin. There
 * are a vast number of possible plugins; {@code Command} plugins are the most
 * common one: they take inputs and produce outputs.
 * </p>
 * <p>
 * A {@link Command} is most useful when it is bound to a menu item; that is
 * what the {@code menuPath} parameter of the {@code @Plugin} annotation does.
 * </p>
 * <p>
 * Each input to the command is specified as a field with the {@code @Parameter}
 * annotation. Each output is specified the same way, but with a
 * {@code @Parameter(type = ItemIO.OUTPUT)} annotation.
 * </p>
 *
 * @author Johannes Schindelin
 * @author Curtis Rueden
 */
@Plugin(type = Command.class, headless = true, menuPath = "Plugins>Deep-STORM>Demo")
public class NetworkDemo implements Command, ActionListener {


    @Parameter(type = ItemIO.INPUT)
    private Dataset input;

    @Parameter(type = ItemIO.OUTPUT)
    private ArrayList<BufferedImage> output;

    @Parameter
    private TensorFlowService tensorFlowService;

    private viewNetwork viewNetwork = new viewNetwork();
    private SavedModelBundle model;


    //When an image is loaded, run your network
    @Override
    public void run() {
        viewNetwork.runMyNetButton.addActionListener(this);
        viewNetwork.setVisible(true);
    }

    //main method for testing purpose.
    public static void main(final String... args) {
        // Launch ImageJ as usual.
        final ImageJ ij = new ImageJ();
        ij.launch();
        final String imagePath = "D:\\repositories\\Deep-STORM\\demo 1 - Simulated Microtubules\\ArtificialDataset_demo1.tif";
        try {
            final Object dataset = ij.io().open(imagePath);
            ij.ui().show(dataset);
        }catch (Exception e){
            System.out.println(e);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (viewNetwork.getModelPath()==null || viewNetwork.getCsvPath()==null){
            JOptionPane.showMessageDialog(null, "Please enter files before pressing run my net button",
                    "warning",JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        viewNetwork.setVisible(false);
        try {
            //opening the saved model.
            String path= viewNetwork.getModelPath();

            String modelPath = viewNetwork.getModelPath();
//            String modelWeights = viewNetwork.getWeightsPath();
            //model = KerasModelImport.importKerasModelAndWeights(modelJson, modelWeights);

            FileLocation location= new FileLocation(modelPath);
            model= tensorFlowService.loadModel(location, "saved_model.pb");
        } catch (Exception exc) {
            JOptionPane.showMessageDialog(null, "Something went wrong trying to open your model.\n" +
                            "Are you sure you chose the right file?",
                    "warning",JOptionPane.INFORMATION_MESSAGE);
            viewNetwork.setVisible(true);
            return;
        }

        double[] meanStd= new double[2];
        try (
                Reader reader = Files.newBufferedReader(Paths.get(viewNetwork.getCsvPath()));
                CSVReader csvReader = new CSVReader(reader);
        ) {
            // Reading Records One by One in a String array
            String[] firstLine=csvReader.readNext();
            if (firstLine==null){
                popCsvErrorMessage();
            }

            String[]secondLine= csvReader.readNext();
            if(secondLine==null || secondLine.length<2){
                popCsvErrorMessage();
            }

            meanStd[0]= Double.parseDouble(secondLine[0]);
            meanStd[1]= Double.parseDouble(secondLine[1]);

        }catch (Exception exc){
            popCsvErrorMessage();
        }

        ArrayList<SimpleMatrix> images;
        try {
            images = Utilities.ReadStackFromTiff(this.input.getSource());
        } catch (Exception exc) {
            System.out.println(exc);
            return;
        }

        images= Utilities.projectImageBetween0and1(images);
        images= Utilities.normalizeImage(images,meanStd[0], meanStd[1]);

        INDArray modelData= Utilities.createModelData(images);

    }



    private void DisplayImage(ArrayList<BufferedImage> images){

        JLabel jLabel = new JLabel(new ImageIcon(images.get(0)));

        JPanel jPanel = new JPanel();
        jPanel.add(jLabel);
        jPanel.setVisible(true);
        for (int i=1; i<=images.size(); i++){
            try {
                Thread.sleep(1000);
            }
            catch (Exception e){

            }

            jLabel= new JLabel(new ImageIcon(images.get(i)));

        }
    }

    private void popCsvErrorMessage(){
        JOptionPane.showMessageDialog(null, "Something went wrong trying to open your csv file.\n" +
                        "Are you sure you chose the right file?",
                "warning",JOptionPane.INFORMATION_MESSAGE);
        viewNetwork.setVisible(true);
    }
}
