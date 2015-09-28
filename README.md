# CS61C Fall 2013 Project 1 - MapReduce on EC2

## Overview

For this project, you're finally going to roll up your sleeves and use MapReduce to answer a big data problem. The question you're going to answer is this: given a word, what other words are statistically associated with it? If I say 'love', or 'death', or 'terrorism', what other words and concepts go with it?

A reasonable statistical definition is the co-occurrence, which measures how often two words appear together in documents. Given a target word, we can figure out which words in a body of text (called the corpus) are most closely related to it by ranking each unique word in the corpus by its co-occurrence rate with the target word. To calculate:

    Let Aw be the number of occurrences of w in the corpus.
    Let Cw be the number of occurrences of w in documents that also have the target word.
    Co-occurrence rate :=  if(Cw > 0)   Cw * (log(Cw))^3  / Aw 
                          else  0

Note that 1) we will use the natural logarithm when calculating co-occurrence and 2) we will not be calculating co-occurrence. Here is an example that illustrates this definition. Let the target word be "Dave":

    Doc_ID#1: Randy, Krste, Dave
    Doc_ID#2: Randy, Dave, Randy
    Doc_ID#3: Randy, Krste, Randy


    Occurrences:           ARandy = 5; AKrste = 2;
    Co-occurrences:        CRandy = 3; CKrste = 1;
    Co-occurrence Rate:    with Randy: CRandy * (log(CRandy))^3 / ARandy = 3/5 * (log(3))3 = 0.7956
                           with Krste: CKrste * (log(CKrste))^3 / AKrste = 1/2 * (log(1))3 = 0

This does nothing to account for the distance between words however. A fairly straightforward generalization of the problem is to, instead of giving each co-occurence a value of 1, give it a value f(d), where d is the minimum distance from the word occurrence to the nearest instance of the target word measured in words. Suppose our target word is cat. The values of d for each word is given below:

    Document:     The fat cat did not like the skinny cat.
    Value of d:    2   1       1   2    3   2     1


If the target word does not exist in the document, the value of d should be Double.POSITIVE_INFINITY.

The function f() takes d as input and outputs another number. We will define f() to send infinity to 0 and positive numbers to numbers greater than or equal to one. The result of the generalization is as follows:

    Let Aw be the number of occurrences of w in the corpus.
    Let Sw be the the sum of f(dw) over all occurrences of w in the corpus.
    Co-occurrence rate :=  if(Sw > 0)   Sw * (log(Sw))^3  / Aw 
                           else  0

Your task is to produce an ordered list of words for the target word sorted by generalized co-occurrence rate, ordered with the biggest co-occurrence rates at the top. The data will be the same data we used in labs 2 and 3. This isn't the most sophisticated text-analysis algorithm out there, but it's enough to illustrate what you can do with MapReduce.


## Part 1 Instructions

In the proj1 directory that you pulled, you will find skeleton code for two MapReduce jobs and a Makefile in a file called Proj1.java and also a file called *DoublePair.java*, which is a custom Writable class. That directory will also include some test data which will contain some of the results from our reference implementation. You should get the same output as we did, approximate to some floating point roundoff.

For part 1, you will need to :

    Implement the DoublePair class in DoublePair.java.
    Implement the MapReduce jobs in Proj1.java to calculate co-occurrence.

### The DoublePair Class
The DoublePair class is a custom Writable class that holds two double variables. Implement all of the functions with the comment //YOUR CODE HERE. You are not required to use DoublePair in your implementation of Proj1.java, but you are required to implement it, and we will test your DoublePair for correctness. Note that if you choose not to use DoublePair, you still should be using some form of custom keys or values. If you would like to use DoublePair as a Hadoop key, you should implement the WritableComparable interface by defining the methods compareTo() and hashCode() (refer to the spec here).

Feel free to write a main method in DoublePair and define tests there. Whether you have a main method or not won't affect any other parts of your project. For a review on custom Hadoop keys/values, please refer to lab 3. If you have written your own main method and would like to test, you can run DoublePair with the following command:

    $ make doublepair

### The MapReduce Jobs

Your solution should implement the generalized co-occurrence algorithm above. Make sure to convert every word you're reading to lowercase first. We will be using three different f(d) functions, the first of which ignores the d parameter and outputs either 0 or 1. Using the first function is the same as using the basic co-occurrence algorithm we described above. To get the value of f(d), you should call the f() method in the Func class. You can select which function is being used by setting the value of the funcNum parameter to either 0, 1, or 2 in conf.xml. While you may define additional f()'s, please do not modify any of the existing ones.

There is also a combiner called Combine1 that can be run between the first Map and first Reduce job. Although implementing this is not required for getting the correct output, implementing this will be worth roughly 10% of your grade. Your combiner must be non-trivial (ie. it must not be the identity function), and you must make sure that your output is the same regardless of whether your combiner is run or not.

You will need to modify some of the type signatures in Proj1.java. Please refer to what you did in lab 2 if you are having trouble. The framework expects you to output (from the second job) a DoubleWritable and Text pair. These should be the score for each word and the word itself, respectively. At the end of your MapReduce jobs, you should output only the first 100 or so results.

Here is a list of potentially helpful info:

- Hadoop guarantees that a reduce tasks receives its keys in sorted order. Note that your co-occurrence values also need to be in sorted order.

- If you do not override a particular map() and/or reduce() function, the mapper and/or reducer for that stage will be the identity function.

- You are free to (and should!) use Java's built-in data structures. In particular, it may be wise to use something other than just Lists.

 - The String and Math classes in Java may come in handy.

### Running Things Locally

You can launch a job via:

    $ hadoop jar proj1.jar Proj1 -conf conf.xml <params> <input> <intermediateDir> <outDir>

You should edit conf.xml to refer to the target word and f for the analysis the three f's we'll be using are numbered 0, 1, and 2). The <input> parameter specifies which input file we are using. The reference solutions were generated from ~cs61c/data/billOfRights.txt.seq.

The params <intermediateDir> and <outDir> specify the intermediate and output directories. These can be whatever you like. The intermediate directory will hold the output from the first MapReduce job, which is used as input for the second job. This may be helpful in debugging.

The <params> field supports two parameters, -Dcombiner and -DrunJob2 Specifying -Dcombiner=true will turn on the combiner, while specifying -Dcombiner=false will turn it off. Specifiying -DrunJob2=false will cause only the first job to run, and will cause its output to be in a friendly (Text) format instead of as sequence files. This is intended for debugging. The default value for -Dcombiner is false and -DrunJob2 is true.

We'll also give you a program (Importer.java) that you can use to convert text files to the appropriate sequence file format. This will let you create your own test data, although it is not required.

Make sure you delete the intermediate and final outputs between runs as to not exceed your disk quotas.

You may submit your project by running:

    $ submit proj1-1


## Part 2 Instructions

We have added a few new files to the project 1 directory. You can get the files by running:

    $ git pull ~cs61c/proj/01 master

PLEASE START EARLY. Only a certain number of clusters can be initialized under the cs61c master account. That means that if you procrastinate on the project right now, there's a good chance that you won't be able to start a cluster easily towards the end of the week, when traffic is heaviest.

### Local Runtime Test

Before you run your code on the cloud, you will need to verify that your code runs fast enough. Real-world MapReduce jobs often process terabytes of data, so it is important that the code is both correct and efficient. For this assignment we won't ask you to produce the most efficient code, but we do want to verify that your code performs reasonably well.

We will run your code locally on a larger file (~cs61c/data/sample.seq) and verify that both your output is correct and that it runs fast enough. We have provided a script called bench.sh to help you test, which you can run via:

    $ ./bench.sh

This script will only report how long your code ran, but it will NOT test your output for correctness. Please make sure that your code finishes within 2 minutes. If your code is slower than that, you should revise your Proj1 design until it passes the benchmark. Note that which word/funcNum you choose will not significantly affect your runtime.

We have provided two reference outputs running (capital, 0) and (capital, 1) on ~cs61c/data/sample.seq. They contain the top 20 words for each run. You can access them here:

    Output for (capital, 0)
    Output for (capital, 1)

### Running Things In The Cloud

We want you to run your job a total of six times in the cloud. We will be running on both the 2005 and 2006 Usenet datasets, which are respectively:

s3n://cs61cUsenet/2005-tmp
s3n://cs61cUsenet/s2006


The format of each tuple below is (<targetWord>, <funcNum>).

    Start 5 EC2 workers and run:
        (freedom, 0) on the 2005 dataset with combiner off

        (freedom, 0) on the 2005 dataset with combiner on

        (capital, 0) on the 2006 dataset with combiner on

    Terminate the 5 EC2 workers, then start 9 EC2 workers and run:
        (capital, 0) on the 2006 dataset with combiner on

        (landmark, 1) on the 2006 dataset with combiner on

        (monument, 2) on the 2006 dataset with combiner on

For each run, be sure to record the following information:

    Total time taken (sum of first and second job)
    The number of mappers and reducers used for each job
    The size of the input (S3N_BYTES_READ)

For the runs on the 2006 dataset, also record:

    The top 20 results (co-occurrence value and word) for each run (see below on how to view). Please save this in a file called <targetWord>-<funcNum>.txt. For example, the output of (capital, 0) should be saved in capital-0.txt

We recommend you save the job status page (eg. as a screenshot) after each run. Once you terminate a cluster, you will not be able to access the logs for that cluster, and neither Hadoop's distributed filesystem.

We estimate that each run with the 9 workers on the 2006 dataset should take around 10 minutes.
Do not leave EC2 machines running when you are not using them. The virtual machines cost the same amount of money when they are active as when they are sitting idle.

### Written Portion
For the final submission, answer the following questions in a file named ec2experience.txt:

1. How long did each of the six runs take? How many mappers and how many reducers did you use?

2. For the two runs with (freedom, 0), how much faster did your code run on the 5 workers with the combiner turned on than with the combiner turned off? Express your answer as a percentage.

3. For the runs on the 2006 dataset, what was the median processing rate of input for the tests using 5 workers? Using 9 workers? Express your answers in terms of GB/s. (1 GB = 2^30 bytes)

4. How much faster was your run of (capital, 0) with 9 workers over 5 workers? Assuming your code is fully parallelizable, how much faster would a run with 9 workers be compared to a run with 5 workers? Express both numbers as a percentage. How well, in your opinion, does Hadoop parallelize your code? Justify your answer in 1-2 sentences.

5. For a single run on the 2006 dataset, what was the price per GB processed on with 5 workers? With 9 workers? (Recall that an extra-large instance costs $0.58 per hour, rounded up to the nearest hour.) Express your answers in terms of dollars/GB.

6. How much total money did you use to complete this project?

### Submission
Submit the following files: Proj1.java, capital-0.txt, landmark-1.txt, monument-2.txt, and ec2experience.txt. You can submit by running:

$ submit proj1-2

### Grading
We're going to grade your code by running it in an automated test harness on sample data. We expect you to give [approximately] the right numeric answers, barring floating point roundoff. So don't modify the three f()'s given. We promise to avoid tests that are overly sensitive to roundoff.

Note again that your code MUST compile for you to receive credit. If you are having trouble implementing a way to find the minimum distance between a word and the target word, you may choose to implement the basic co-occurrence algorithm only for partial credit.
Part 1 Rubric

10 pts - Implement a working DoublePair. You will only need to implement Writable, not WritableComparable for points.
15 pts - Correctly calculate basic co-occurrences (ie. using funcNum = 0)
15 pts - Correctly calculate generalized co-occurrence based on word distances (ie. using funcNum 1 and 2)
10 pts - Implementing a non-trivial combiner

Total: 50 points