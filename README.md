# collab
Collaborative design tool

## Introduction

This project defines a simple graphical user interface for a multi-actor collaborative design study with surrogate parameter design tasks. It includes a manager GUI to control the advancement of tasks and a designer GUI to modify input variables and visualize output variables.

## Pre-requisites

You must have a runtime infrastructure (RTI) for the IEEE Std. 1516-2010 High Level Architecture (HLA). The included Maven configuration (pom.xml) assumes you are using version 2.0.2 of the open source Portico RTI with the environment variable RTI_HOME set to the install path.

## Running the Program

The main class `DebugMain` launches one manager GUI and three designer GUIs. Note that Portico uses UDP multicast transmission which is often filtered/blocked by network hardware. Consider using a local area network or private wireless network to allow each client to communicate with others.

When launching clients independently, the main classes are `ManagerMain` and `DesignerMain`. Note the manager must be launched first to correctly register designers. Sample experiments are defined in the `src/generator` directory.

## References

P.T. Grogan and O.L. de Weck, "Collaboration and complexity: An experiment on the effect of multi-actor coupled design," *Research in Engineering Design*, 2016. [Early access](http://dx.doi.org/10.1007/s00163-016-0214-7). 