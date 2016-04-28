"""
Copyright 2016 Paul T. Grogan, Stevens Institute of Technology

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""

import json
import math
import re

class PostProcessor(object):
    """
    Performs post-processing functions on experimental data.
    """
    def __init__(self):
        """
        Initializes this post-processor.
        """
        self.session = None
        
    def loadSession(self, rootPath, sessionName):
        """
        Loads experimental results from a configured session.
        
        @param rootPath: the file path root
        @type rootPath: str
        
        @param sessionName: the session name
        @type sessionName: str
        """
        with open('config.json') as configData:
            config = json.load(configData)
            session = next(s for s in config["sessions"] if s["name"] == sessionName)
            self.loadFile(rootPath+'/'+sessionName+'/'+session["jsonFile"],
                          rootPath+'/'+sessionName+'/'+session["logFile"],
                          session["errTolerance"],
                          session["numTolerance"])
            map(self.session.tasks.remove,
                [task for task in self.session.tasks if task.order in session["omittedTasks"]])
        
    def loadFile(self, jsonFile, logFile, errTolerance = 0.05, numTolerance = 0.005):
        """
        Loads experimental results from file.
        
        @param jsonFile: the experimental json file
        @type jsonFile: str
        
        @param logFile: the experimental log file
        @type logFile: str
        """
        
        # parse json file to instantiate experiments, tasks, and problems
        with open(jsonFile) as jsonData:
            session = json.load(jsonData)
        
            self.session = Session(
                name = session["name"],
                errTolerance = session["errTolerance"]
                if "errTolerance" in session else errTolerance,
                numTolerance = session["numTolerance"]
                if "numTolerance" in session else numTolerance,
                trainingTasks = map(lambda t: Task.parse(t[0], t[1]),
                                    zip(session["trainingModels"],
                                        range(1, 1 + len(session["trainingModels"])))),
                tasks = map(lambda t: Task.parse(t[0], t[1]),
                            zip(session["experimentModels"],
                                range(1, 1 + len(session["experimentModels"]))))
            )
        
        # parse log file to instantiate actions
        with open(logFile) as logData:
            task = None
            # iterate over each line in log
            for line in logData.read().splitlines():
                # parse time, type, and content fields
                data = line.split(',')
                time = long(data[0])
                type = data[1]
                content = data[2]
                
                # handle opened event: check for session match
                if type == 'opened':
                    if content != self.session.name:
                        raise Exception('Mis-matched sessions ({}, {}).'.format(
                            content, self.session.name))
                # handle initialized event: append initial action to corresponding task
                elif type == 'initialized':
                    # parse the name and target values
                    result = re.match('name="(?P<name>.+)"; target={(?P<target>.+)}', content)
                    if result:
                        name = result.group('name')
                        target = map(float, result.group('target').split(';'))
                        # find the task problem which matches the name
                        task = next((t for t in self.session.trainingTasks + self.session.tasks
                                     if t.problem.name == name))
                        if task is None:
                            raise Exception('Missing task problem ().'.format(name))
                        # set task time
                        task.time = time
                        # append an action with initial inputs and outputs
                        task.actions.append(Action(time = time, input = [0]*len(target),
                                                output = [0]*len(target)))
                # handle updated event: append new action to corresponding task
                elif type == 'updated':
                    if task is None:
                        raise Exception('Task not initialized.')
                    # parse the input and output values
                    result = re.match('input={(?P<input>.+)}; output={(?P<output>.+)}', content)
                    if result:
                        input = map(float, result.group('input').split(';'))
                        output = map(float, result.group('output').split(';'))
                        # append an action with updated inputs and outputs
                        task.actions.append(Action(time = time, input = input,
                                                output = output))
                # handle solved event: reset task
                elif type == 'solved':
                    # log completion time and reset task
                    task.completionTime = time
                    task = None

class Session(object):
    """
    An experimental session. Includes settings and the
    list of tasks (training and experimental).
    """
    def __init__(self, name='', errTolerance=0.05, numTolerance=0.0,
                 trainingTasks = [], tasks = []):
        """
        Initializes this session.
        
        @param name: the session name
        @type name: str
        
        @param errTolerance: the error tolerance for solutions
        @type errTolerance: float
        
        @param numTolerance: the numerical tolerance for solution checking
        @type numTolerance: float
        
        @param trainingTasks: the training tasks
        @type trainingTasks: array(Task)
        
        @param tasks: the experimental tasks
        @type tasks: array(Task)
        """
        self.name = name
        self.errTolerance = errTolerance
        self.numTolerance = numTolerance
        self.trainingTasks = trainingTasks
        self.tasks = tasks
    

class Task(object):
    """
    An experimental task. Includes the technical problem and a list of actions.
    """
    def __init__(self, problem, order):
        """
        Initializes this task.
        
        @param problem: the technical problem
        @type problem: Problem
        
        @param order: the experimental order
        @type order: int
        """
        self.problem = problem
        self.order = order
        self.actions = []
        self.time = 0
        self.completionTime = 0
        
    def getInitialTime(self, designer = None):
        """
        Gets the initial time (time of first action) for this task.
        
        @param designer: the designer
        @type designer: int
        
        @returns: the initial time (milliseconds)
        @rtype: long
        """
        # if no designer specified, return initial time for entire task
        if designer is None:
            initialAction = next(a for a in self.actions
                                 if any(i != 0 for i in a.getInput(self)))
            return initialAction.time
        # if designer does not participate, return default time
        elif designer not in self.problem.inputs:
            return self.time
        # if individual task, return initial time for specified designer
        elif not self.problem.isTeam():
            initialAction = next(a for a in self.actions
                                 if any(i != 0 for i in a.getInput(self, designer)))
            return initialAction.time
        # otherwise return time for entire task
        else:
            return self.getInitialTime()
    
    def getCompletionTime(self, session, designer = None):
        """
        Gets the completion time (time of last action) for this task.
        
        @param session: the experimental session
        @type session: Session
        
        @param designer: the designer
        @type designer: int
        
        @returns: the completion time (milliseconds)
        @rtype: long
        """
        # by default, the task is complete at the first action solving the task
        if any(a.isSolved(session, self, designer) for a in self.actions):
            finalAction = next(a for a in self.actions
                               if a.isSolved(session, self, designer))
            return finalAction.time
        # however, sometimes the "solution" is missed due to numerical issues
        else:
            # team tasks are completed at the overall completion time
            if self.problem.isTeam():
                return self.completionTime
            # individual tasks fail "gracefully"
            else:
                return self.getInitialTime(designer) - 1000
    
    def getElapsedTime(self, session, designer = None):
        """
        Gets the elapsed time (duration) for this task.
        
        @param session: the experimental session
        @type session: Session
        
        @param designer: the designer
        @type designer: int
        
        @returns: the elapsed time (milliseconds)
        @rtype: long
        """
        return self.getCompletionTime(session, designer) - self.getInitialTime(designer)
    
    @staticmethod
    def parse(json, order):
        """
        Parses a Task from json data.
        
        @param json: the json data
        @type json: dict
        
        @param order: the task order
        @type order: int
        """
        return Task(problem = Problem.parse(json), order = order)

class Action(object):
    """
    An experimental action.
    """
    def __init__(self, time = 0, input = [], output = []):
        """
        Initializes this action.
        
        @param time: the action time (milliseconds)
        @type time: long
        
        @param input: the resulting input vector
        @type input: array(float)
        
        @param output: the resulting output vector
        @type output: array(float)
        """
        self.time = time
        self.input = input
        self.output = output
    
    def getError(self, task, designer = None):
        """
        Gets the error in design after this action for a task.
        
        @param task: the task
        @type task: Task
        
        @param designer: the designer
        @type designer: int
        
        @returns: the error
        @rtype: array(float)
        """
        # compute error as outputs - targets
        return [o - t for o, t in zip(self.getOutput(task, designer),
                                      task.problem.getTarget(designer))]
            
    def getErrorNorm(self, designer = None):
        """
        Gets the error norm in design after this action for a task.
        
        @param task: the task
        @type task: Task
        
        @param designer: the designer
        @type designer: int
        
        @returns: the error norm
        @rtype: float
        """
        # compute norm as sqrt(sum(abs(error)^2))
        return math.sqrt(sum(map(lambda i: math.pow(abs(i), 2),
                                 self.getError(task, designer))))
    
    def getElapsedTime(self, task, designer = None):
        """
        Gets the elapsed time of this action.
        
        @param task: the task
        @type task: Task
        
        @param designer: the designer
        @type designer: int
        
        @returns: the elapsed time (milliseconds)
        @rtype: long
        """
        return self.time - task.getInitialTime(designer)
    
    def isSolved(self, session, task, designer = None):
        """
        Checks if the task is solved.
        
        @param session: the experimental session
        @type session: Session
        
        @param task: the task
        @type task: Task
        
        @param designer: the designer
        @type designer: int
        
        @returns: true, if this action solves the task
        @rtype: bool
        """
        if(not task.problem.isTeam()):
            return all(abs(e) < session.errTolerance + session.numTolerance
                       for e in self.getError(task, designer))
        else:
            return all(abs(e) < session.errTolerance + session.numTolerance
                       for e in self.getError(task))
    
    def getInput(self, task, designer = None):
        """
        Gets the input for a designer.
        
        @param task: the task
        @type task: Task
        
        @param designer: the designer
        @type designer: int
        
        @returns: the input vector
        @rtype: array(float)
        """
        if designer is None:
            return self.input
        else:
            # filter the input to only return those assigned to designer
            return [self.input[i] for i, d in
                    enumerate(task.problem.inputs) if d == designer]
    
    def getOutput(self, task, designer = None):
        """
        Gets the output for a designer.
        
        @param task: the task
        @type task: Task
        
        @param designer: the designer
        @type designer: int
        
        @returns: the output vector
        @rtype: array(float)
        """
        if designer is None:
            return self.output
        else:
            # filter the output to only return those assigned to designer
            return [self.output[i] for i, d in
                    enumerate(task.problem.outputs) if d == designer]

class Problem(object):
    """
    A tasked problem.
    """
    def __init__(self, name, coupling, target, inputs, outputs,
                 inputLabels, outputLabels):
        """
        Initializes this problem.
        
        @param name: the name
        @type name: str
        
        @param coupling: the coupling matrix (M)
        @type coupling: array(array(float))
        
        @param target: the target vector (y_star)
        @type target: array(float)
        
        @param inputs: the input assignments
        @type inputs: array(int)
        
        @param outputs: the output assignments
        @type outputs: array(int)
        
        @param inputLabels: the input labels
        @type inputLabels: array(str)
        
        @param outputLabels: the output labels
        @type outputLabels: array(str)
        """
        self.name = name
        self.coupling = coupling
        self.target = target
        self.inputs = inputs
        self.outputs = outputs
        self.inputLabels = inputLabels
        self.outputLabels = outputLabels
    
    def getTarget(self, designer = None):
        """
        Gets the target vector.
        
        @param designer: the designer assigned to targets
        @type designer: int
        """
        if designer is None:
            return self.target
        else:
            # filter the target to only return those assigned to designer
            return [self.target[i] for i, d in
                    enumerate(self.outputs) if d == designer]
    
    def isCoupled(self):
        """
        Checks if this problem is coupled.
        
        @return true, if this problem is coupled
        @rtype bool
        """
        for i, row in enumerate(self.coupling):
            for j, cell in enumerate(self.coupling[i]):
                if i is not j and self.coupling[i][j] is not 0:
                    return True
        return False
        
    def getTeamSize(self):
        """
        Gets the team size for this problem.
        
        @return the number of team members
        @rtype int
        """
        if 'Individual' in self.name:
            return 1
        else:
            # count number of designers assigned to inputs or outputs
            # compute using length of a set (removes duplicates)
            return max(len(set(self.inputs)), len(set(self.outputs)))
    
    def isTeam(self):
        """
        Checks if this is a team task.
        
        @returns: true, if this is a team task
        @rtype: bool
        """
        return self.getTeamSize() > 1
    
    def getProblemSize(self):
        """
        Gets the problem size for this problem.
        
        @return the number of inputs and outputs
        @rtype int
        """
        if 'Team' in self.name:
            # count number of inputs / ouputs
            return max(len(self.inputs), len(self.outputs))
        else:
            # divide number of inputs / outputs by number of different designers
            # compute using length of a set (removes duplicates)
            return (max(len(self.inputs), len(self.outputs))
                    / max(len(set(self.inputs)), len(set(self.outputs))))
    
    @staticmethod
    def parse(json):
        """
        Parses a Problem from a JSON structure.
        
        @param json: the json data
        @type json: dict
        """
        # generate default input labels if missing
        inputLabels = (json["inputLabels"]
                       if "inputLabels" in json
                       else map(lambda i: 'x_'+str(i),
                                range(1, 1 + len(json["targetVector"]))))
        
        # generate default output labels if missing
        outputLabels = (json["outputLabels"]
                        if "outputLabels" in json
                        else map(lambda i: 'y_'+str(i),
                                 range(1, 1 + len(json["targetVector"]))))
        
        # parse input assignments from input indices:
        # for each designer's list of assigned indices
        # add that designer's index to corresponding input assignment
        inputs = [0]*len(inputLabels)
        for d, designer in enumerate(json["inputIndices"]):
            for index in designer:
                inputs[index] = d
        
        # parse output assignments from output indices:
        # for each designer's list of assigned indices
        # add that designer's index to the corresponding output assignment
        outputs = [0]*len(outputLabels)
        for d, designer in enumerate(json["outputIndices"]):
            for index in designer:
                outputs[index] = d
        
        # return the parsed problem object
        return Problem(
            name = json["name"],
            coupling = json["couplingMatrix"],
            target = json["targetVector"],
            inputs = inputs,
            outputs = outputs,
            inputLabels = inputLabels,
            outputLabels = outputLabels
        )

import sys, os
import argparse

if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description = "This program post-processes experimental data."
    )
    parser.add_argument('-r', '--root', type = str, required = False,
                        help = 'Experiment root file path')
    parser.add_argument('-s', '--session', type = str, required = False,
                        help = 'Experiment session name')
    parser.add_argument('-j', '--json', type = str, required = False,
                        help = 'Experiment json file path')
    parser.add_argument('-l', '--log', type = str, required = False,
                        help = 'Experiment log file path')
    parser.add_argument('-e', '--errTol', type = float, required = False,
                        help = 'Error tolerance')
    parser.add_argument('-n', '--numTol', type = float, required = False,
                        help = 'Numerical tolerance')
    args = parser.parse_args()
    pp = PostProcessor()
    if args.root and args.session:
        pp.loadSession(args.root, args.session)
    elif args.json and args.log:
        if args.errTol:
            if args.numTol:
                pp.loadFile(args.json, args.log,
                            errTolerance = args.errTol,
                            numTolerance = args.numTol)
            else:
                pp.loadFile(args.json, args.log,
                            errTolerance = args.errTol)
        else:
            if args.numTol:
                pp.loadFile(args.json, args.log,
                            numTolerance = args.numTol)
            else:
                pp.loadFile(args.json, args.log)
    # print header
    print pp.session.name
    print "epsilon = " + '{:3.2f}'.format(pp.session.errTolerance)
    print "{0:>3} {1:>3} {2:>3} {3:>3} {4:>10} {5:>10} {6:>10} {7:>10}".format(
        "O", "C/U", "N", "n", "Team (s)", "Red (s)", "Green (s)", "Blue (s)")
    # print rows for each task
    for task in pp.session.tasks:
        print "{0:>3} {1:>3} {2:>3} {3:>3} {4:>10} {5:>10} {6:>10} {7:>10}".format(
            task.order,
            'C' if task.problem.isCoupled() else 'U',
            task.problem.getProblemSize(),
            task.problem.getTeamSize(),
            "{:10.2f}".format(task.getElapsedTime(pp.session)/1000.)
            if task.problem.isTeam() else '',
            "{:10.2f}".format(task.getElapsedTime(pp.session, 0)/1000.)
            if not task.problem.isTeam() else '',
            "{:10.2f}".format(task.getElapsedTime(pp.session, 1)/1000.)
            if not task.problem.isTeam() else '',
            "{:10.2f}".format(task.getElapsedTime(pp.session, 2)/1000.)
            if not task.problem.isTeam() else ''
        )