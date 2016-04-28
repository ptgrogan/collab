function [value, alpha, beta, gamma, A] = totComplexity(model)

A = (model.T ~= 0) - (diag(diag(model.T)) ~= 0);
alpha = [ones(length(model.M), 1); zeros(length(model.D), 1)];
beta = [ones(size(model.M)) zeros(size(transpose(model.O))); ...
    zeros(size(model.I)) (ones(size(model.D))-eye(size(model.D)))];
gamma = 1;

value = sum(alpha) + sum(sum(beta.*A))*gamma*sum(svd(A));