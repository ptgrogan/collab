classdef TaskFactory < handle
    properties (SetAccess = private, GetAccess = public)
        name = '';
        desc = '';
        number = 0;
        coupled = 0;
        inputs = [];
        outputs = [];
        inputLabels = {};
        outputLabels = {};
    end
    methods
        function obj = TaskFactory(name, desc, number, coupled, ...
                inputs, outputs, inputLabels, outputLabels)
            obj.name = name;
            obj.desc = desc;
            obj.number = number;
            obj.coupled = coupled;
            obj.inputs = inputs;
            obj.outputs = outputs;
            
            if isempty(inputLabels)
                obj.inputLabels = cell(sum(number),1);
                for i=1:sum(number)
                    obj.inputLabels{i} = ['X' num2str(i)];
                end
            else
                obj.inputLabels = inputLabels;
            end
            if isempty(outputLabels)
                obj.outputLabels = cell(sum(number),1);
                for i=1:sum(number)
                    obj.outputLabels{i} = ['Y' num2str(i)];
                end
            else
                obj.outputLabels = outputLabels;
            end
        end
        
        function model = generate(obj)
            model = SystemModel(obj.name, obj.desc, obj.number, ...
                obj.coupled, obj.inputs, obj.outputs, ...
                obj.inputLabels, obj.outputLabels);
        end
    end
end
    