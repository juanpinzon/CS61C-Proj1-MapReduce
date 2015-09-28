/*
 * CS61C Fall 2013 Project 1 - Project1.java
 * @version 1	Sept 20 2013
 * @author Juan Pinzon		login:cs61c-vy		StudentID:23632316
 *
 * Given a word, what other words are statistically associated with it? If I say 'love', or 'death', or 'terrorism', what other words and concepts go with it? 
 * Produce an ordered list of words for the target word sorted by generalized co-occurrence rate, ordered with the biggest co-occurrence rates at the top. The data will be the
 * same data we used in labs 2 and 3. This isn't the most sophisticated text-analysis algorithm out there, but it's enough to illustrate what you can do with MapReduce.
**/

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.Math;


import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

/*
 * This is the skeleton for CS61c project 1, Fall 2013.
 *
 * Reminder:  DO NOT SHARE CODE OR ALLOW ANOTHER STUDENT TO READ YOURS.
 * EVEN FOR DEBUGGING. THIS MEANS YOU.
 * 
 */
public class Proj1{
    

    /*
     * Inputs is a set of (docID, document contents) pairs.
     */
    public static class Map1 extends Mapper<WritableComparable, Text, Text, DoublePair> {
        /** Regex pattern to find words (alphanumeric + _). */
        final static Pattern WORD_PATTERN = Pattern.compile("\\w+");

        private String targetGram = null;
        private int funcNum = 0;

        /*
         * Setup gets called exactly once for each mapper, before map() gets called the first time.
         * It's a good place to do configuration or setup that can be shared across many calls to map
         */
        @Override
            public void setup(Context context) {
                targetGram = context.getConfiguration().get("targetWord").toLowerCase();
                try {
                    funcNum = Integer.parseInt(context.getConfiguration().get("funcNum"));
                } catch (NumberFormatException e) {
                    /* Do nothing. */
                }
            }

        @Override
        public void map(WritableComparable docID, Text docContents, Context context)
            throws IOException, InterruptedException {
                
            Matcher matcher = WORD_PATTERN.matcher(docContents.toString());
            Func func = funcFromNum(funcNum);

            // YOUR CODE HERE
            ArrayList<String> doc_words = new ArrayList<String>();                      //Store all words within the document.
            ArrayList<Double> targetGram_pos = new ArrayList<Double>();                 //Store the index of each occurrence of target word in the document
                
            DoublePair values = new DoublePair();                                       //DoublePair that store distance, ocurrences
            values.setDouble2(new Double(1.0));                                         //ocurrences = 0
            Text output = new Text();
                
            //Store each word within the document in doc_words
            while (matcher.find()) {            
                doc_words.add( new String(matcher.group().toLowerCase()) );
            }
                
            //Traverse the document and store each word within it in ArrayList doc_words, and at the same time store the index of each occurence of target word within the document in targetGram_pos
            for (int i = 0; i < doc_words.size(); i++) {
                String word = doc_words.get(i);
                if(word.equals(targetGram))
                    targetGram_pos.add(new Double(i));
            }
                
                
            //Traverse the doc_words ArrayList and find the distance between each word within the document and the target word
            //If there were not any ocurrence of target word distance is 0 to all words
            int index_tw = 0;						                                    //index target word
            Double distance = new Double(0);                                            //store the distance between current word and target word
            for(int i = 0; i < doc_words.size(); i++) { 
                if (targetGram_pos.size() == 0) {										//If target word is not within the document, distance for all words is Double.POSITIVE_INFINITY
                	distance = Double.POSITIVE_INFINITY;				
                }
                else {
                	if(doc_words.get(i).equals(targetGram)) {			                //If word within the document is the same target word skip it and go to the next word
                		continue;
                	}
                	if(targetGram_pos.size() == 1) {					                //If there were just one entre of the target word
                		distance = Math.abs(i - targetGram_pos.get(index_tw));
                	}					
                	else {
                		if(index_tw < targetGram_pos.size()-1) {		                //If this is not the LAST position of the ArrayList of indexes of the target word
                			if( Math.abs(i - targetGram_pos.get(index_tw)) > Math.abs(i - targetGram_pos.get(index_tw+1)) ) {           //Compare the lowest distance between the nearest two indexes 
                				index_tw++;
                			}                			
                		}
                		distance = Math.abs(i - targetGram_pos.get(index_tw));						
                	}                    
                }
                values.setDouble1(new Double( func.f(distance) ));                      //Evaluate dist on f(d) and store it on distance.d1
                output.set(doc_words.get(i));                                           //Output key is each word
                context.write(output, values);                                          //key, value: key: each word, value:Pair of Double(distance, num of co-currences)
            }//end for
                                
        }//end map1
            

            
        /** Returns the Func corresponding to FUNCNUM*/
        private Func funcFromNum(int funcNum) {
            Func func = null;
            switch (funcNum) {
                case 0:	
                    func = new Func() {
                        public double f(double d) {
                            return d == Double.POSITIVE_INFINITY ? 0.0 : 1.0;
                        }			
                    };	
                    break;
                case 1:
                    func = new Func() {
                        public double f(double d) {
                            return d == Double.POSITIVE_INFINITY ? 0.0 : 1.0 + 1.0 / d;
                        }			
                    };
                    break;
                case 2:
                    func = new Func() {
                        public double f(double d) {
                            return d == Double.POSITIVE_INFINITY ? 0.0 : 1.0 + Math.sqrt(d);
                        }			
                    };
                    break;
            }
            return func;
        }
    }

    /** Here's where you'll be implementing your combiner. It must be non-trivial for you to receive credit. */
    public static class Combine1 extends Reducer<Text, DoublePair, Text, DoublePair> {

        @Override
        public void reduce(Text key, Iterable<DoublePair> values,
                    Context context) throws IOException, InterruptedException {
            // YOUR CODE HERE
            //Add DoublePair values(distance, ocurrences) for the document, before return map to the master
            Double total_distance = new Double(0.0);
            Double total_ocu = new Double(0.0);
            for (DoublePair value : values) {
                total_distance += new Double(value.getDouble1());
                total_ocu += new Double(value.getDouble2());
            }
            context.write(key, new DoublePair(total_distance, total_ocu));

        }//end combine1
    }


    public static class Reduce1 extends Reducer<Text, DoublePair, DoubleWritable, Text> {
        @Override
        public void reduce(Text key, Iterable<DoublePair> values,
                    Context context) throws IOException, InterruptedException {
            // YOUR CODE HERE
            //Add DoublePair values(distance, ocurrences) for the whole corpus
            Double total_distance = new Double(0.0);
            Double total_ocu = new Double(0.0);
            for (DoublePair value : values) {
                total_distance += value.getDouble1();
                total_ocu += value.getDouble2();
            }
            
            //Calculate occurrence rate
            Double result = new Double(0.0);
            if (total_distance != 0)
                result = ((total_distance * Math.pow(Math.log(total_distance), 3))/total_ocu) * -1;        
        
            context.write(new DoubleWritable(result), key);
        }
    }

    public static class Map2 extends Mapper<DoubleWritable, Text, DoubleWritable, Text> {
        //maybe do something, maybe don't
        public void map(DoubleWritable co_ocurrence_rate, Text word, Context context)
                        throws IOException, InterruptedException {
            //sort by order of co-occurrence rate, passing the data from reduce1 through reduce2
            context.write(co_ocurrence_rate, word);
        }
    }

    public static class Reduce2 extends Reducer<DoubleWritable, Text, DoubleWritable, Text> {

        int n = 0;
        static int N_TO_OUTPUT = 100;

        /*
         * Setup gets called exactly once for each reducer, before reduce() gets called the first time.
         * It's a good place to do configuration or setup that can be shared across many calls to reduce
         */
        @Override
            protected void setup(Context c) {
                n = 0;
            }

        /*
         * Your output should be a in the form of (DoubleWritable score, Text word)
         * where score is the co-occurrence value for the word. Your output should be
         * sorted from largest co-occurrence to smallest co-occurrence.
         */
        @Override
            public void reduce(DoubleWritable key, Iterable<Text> values,
                    Context context) throws IOException, InterruptedException {

                // YOUR CODE HERE
                int amount = 0;            
                for (Text gram : values) {
                    key.set(new Double(Math.abs(key.get())));
                    context.write(key, gram);
                    amount++;
                
					//When reach N_TO_OUTPUT stop
                    if (amount == N_TO_OUTPUT)
                        break;                
                }

            }
    }

    /*
     *  You shouldn't need to modify this function much. If you think you have a good reason to,
     *  you might want to discuss with staff.
     *
     *  The skeleton supports several options.
     *  if you set runJob2 to false, only the first job will run and output will be
     *  in TextFile format, instead of SequenceFile. This is intended as a debugging aid.
     *
     *  If you set combiner to false, the combiner will not run. This is also
     *  intended as a debugging aid. Turning on and off the combiner shouldn't alter
     *  your results. Since the framework doesn't make promises about when it'll
     *  invoke combiners, it's an error to assume anything about how many times
     *  values will be combined.
     */
    public static void main(String[] rawArgs) throws Exception {
        GenericOptionsParser parser = new GenericOptionsParser(rawArgs);
        Configuration conf = parser.getConfiguration();
        String[] args = parser.getRemainingArgs();

        boolean runJob2 = conf.getBoolean("runJob2", true);
        boolean combiner = conf.getBoolean("combiner", false);

        System.out.println("Target word: " + conf.get("targetWord"));
        System.out.println("Function num: " + conf.get("funcNum"));

        if(runJob2)
            System.out.println("running both jobs");
        else
            System.out.println("for debugging, only running job 1");

        if(combiner)
            System.out.println("using combiner");
        else
            System.out.println("NOT using combiner");

        Path inputPath = new Path(args[0]);
        Path middleOut = new Path(args[1]);
        Path finalOut = new Path(args[2]);
        FileSystem hdfs = middleOut.getFileSystem(conf);
        int reduceCount = conf.getInt("reduces", 32);

        if(hdfs.exists(middleOut)) {
            System.err.println("can't run: " + middleOut.toUri().toString() + " already exists");
            System.exit(1);
        }
        if(finalOut.getFileSystem(conf).exists(finalOut) ) {
            System.err.println("can't run: " + finalOut.toUri().toString() + " already exists");
            System.exit(1);
        }

        {
            Job firstJob = new Job(conf, "job1");

            firstJob.setJarByClass(Map1.class);

            /* You may need to change things here */
            firstJob.setMapOutputKeyClass(Text.class);
            firstJob.setMapOutputValueClass(DoublePair.class);              //Change Map output Value type to DoublePair
            firstJob.setOutputKeyClass(DoubleWritable.class);
            firstJob.setOutputValueClass(Text.class);
            /* End region where we expect you to perhaps need to change things. */

            firstJob.setMapperClass(Map1.class);
            firstJob.setReducerClass(Reduce1.class);
            firstJob.setNumReduceTasks(reduceCount);


            if(combiner)
                firstJob.setCombinerClass(Combine1.class);

            firstJob.setInputFormatClass(SequenceFileInputFormat.class);
            if(runJob2)
                firstJob.setOutputFormatClass(SequenceFileOutputFormat.class);

            FileInputFormat.addInputPath(firstJob, inputPath);
            FileOutputFormat.setOutputPath(firstJob, middleOut);

            firstJob.waitForCompletion(true);
        }

        if(runJob2) {
            Job secondJob = new Job(conf, "job2");

            secondJob.setJarByClass(Map1.class);
            /* You may need to change things here */
            secondJob.setMapOutputKeyClass(DoubleWritable.class);
            secondJob.setMapOutputValueClass(Text.class);
            secondJob.setOutputKeyClass(DoubleWritable.class);
            secondJob.setOutputValueClass(Text.class);
            /* End region where we expect you to perhaps need to change things. */

            secondJob.setMapperClass(Map2.class);
            secondJob.setReducerClass(Reduce2.class);

            secondJob.setInputFormatClass(SequenceFileInputFormat.class);
            secondJob.setOutputFormatClass(TextOutputFormat.class);
            secondJob.setNumReduceTasks(1);


            FileInputFormat.addInputPath(secondJob, middleOut);
            FileOutputFormat.setOutputPath(secondJob, finalOut);

            secondJob.waitForCompletion(true);
        }
    }

}
