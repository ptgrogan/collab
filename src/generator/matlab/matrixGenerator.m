function [matrix, target] = matrixGenerator(number, coupled)

% create an empty matrix in which to build the generated matrix
matrix = [];

% create an empty target vector in which to store the target vector
target = zeros(sum(number),1);

identicalSubTasks = false;

taskMatrix = [];
taskVector = [];
taskSolution = [];

% for each of the specified sub-task sizes (number of inputs/outputs)
for i=1:length(number)
    if ~identicalSubTasks || i == 1
        % only generate new values for first sub-task or for non-identical
        % sub-task settings
        
        if coupled(i)
            % if the sub-task is coupled, generate a matrix component using a
            % random orthonormal matrix
            taskMatrix = orth(rand(number(i), number(i)));
        else
            % if the sub-task is uncoupled, generate a matrix component using a
            % diagonal unit matrix with random flips between 1 and -1
            taskMatrix = diag(2*round(rand(number(i),1))-1); 
        end

        % create a random orthonormal vector of the proper size
        taskVector = orth(rand(number(i),1));
        
        taskSolution = taskMatrix\taskVector;

        % re-assign target vector as long as at least one target 
        % value is within initial condition
        while any(abs(taskSolution) <= 0.05)
            taskVector = orth(rand(number(i),1));
            taskSolution = taskMatrix\taskVector;
        end
    end
    
    matrix = [[matrix; zeros(number(i), size(matrix, 1))] ...
        [zeros(size(matrix, 2), number(i)); taskMatrix]]; 
    
    % set target indices corresponding to this number to a random
    % orthonormal vector of the proper size
    target(1+sum(number(1:i-1)):sum(number(1:i))) = taskVector;
end
