# Collaborative Design Post-processor

Paul T. Grogan, [pgrogan@stevens.edu](mailto:pgrogan@stevens.edu)

## Introduction

This software post-processes data generated from multi-actor parameter design tasks in the collaborative design study developed by Grogan in 2013 and subsequently published in Grogan and de Weck (2016). The data from these experiments takes two forms: a JSON file which describes the technical problems in the experiment (couplng matrices, input and output assignments, and labels), and a log file which records the actions taken by participants during the experimental session.

This software contains two components: a post-processor which loads data from the JSON and log files into Python data structures, and an executable script which, by default, computes and prints completion time data for tasks in an experimental session.

## Pre-requisites

This software is written in Python and requires Python version 2.7. The authors recommend an integrated Python environment such as Anaconda for ease of use.

## Usage

The executable script is currently the primary interface to this software. To run it, open a command/terminal window. The script `post_process.py` has two operating modes:

1. Pre-configured Sessions, e.g. `python post_process.py -r /path/to/data -s sessionName`

This operating mode is only valid for session names defined in the `config.json` file which specifies JSON and log file locations, error and numerical tolerances specific to existing data, and a list of any tasks to be omitted from data analysis.

For example, to post-process test data, execute:
`python post_process.py -r /path/to/data -s test`

2. Unconfigured Sessions, e.g. `python post_process.py -j /path/to/json -l /path/to/log [-e errTol] [-n numTol]`

This operating mode can be used for other experimental data. The error tolerance (errTol) defines the acceptable error of solutions to the center of the target region (defaults to 0.05). The numerical tolerance (numTol) deals with numerical issues with insufficient precision in data logs encountered in early sessions (defaults to 0.000).

For example, to post-process test data, execute:
`python post_process.py -j /path/to/data/test/test.json -l /path/to/data/test/test.log -e 0.05 -n 0.000`

## References

Grogan, P.T. and O.L. de Weck, "Collaboration and complexity: an experiment on the effect of multi-actor coupled design," *Research in Engineering Design*, 2016. [Early access](http://link.springer.com/article/10.1007%2Fs00163-016-0214-7).