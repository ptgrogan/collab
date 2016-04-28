classdef SystemModel < handle
    properties
        name = '';
        desc = '';
        M = [];
        y_star = [];
        inputs = [];
        outputs = [];
        I = [];
        O = [];
        D = [];
        T = [];
        C_t = 0;
        C_s = 0;
        C = 0;
        inputLabels = {};
        outputLabels = {};
    end
    methods
        function obj = SystemModel(name, desc, number, coupled, ...
                inputs, outputs, inputLabels, outputLabels)
            obj.name = name;
            obj.desc = desc;
            [obj.M, obj.y_star] = matrixGenerator(number, coupled);
            
            obj.inputs = inputs;
            obj.outputs = outputs;
            
            obj.I = zeros(length(inputs), sum(number));
            for i=1:length(inputs)
                for j=1:length(inputs{i})
                    if inputs{i}(j)+1 <= sum(number)
                        obj.I(i,inputs{i}(j)+1) = 1;
                    end
                end
            end
            obj.O = zeros(length(outputs), sum(number));
            for i=1:length(inputs)
                for j=1:length(inputs{i})
                    if outputs{i}(j)+1 <= sum(number)
                        obj.O(i,outputs{i}(j)+1) = 1;
                    end
                end
            end
            obj.D = obj.I*obj.M*transpose(obj.O);
            obj.T = [obj.M transpose(obj.O); obj.I zeros(size(obj.D))];
            if length(number) > 1
                singleModel = SystemModel(name, desc, number(1), ...
                    coupled(1), inputs(1), outputs(1), cell(0,0), cell(0,0));
                obj.C_t = singleModel.C_t;
                obj.C_s = singleModel.C_s;
                obj.C = singleModel.C;
            else
                obj.C_t = techComplexity(obj);
                obj.C_s = socComplexity(obj);
                obj.C = totComplexity(obj);
            end
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
    end
end
    