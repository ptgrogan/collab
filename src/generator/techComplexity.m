function [value, alpha, beta, gamma, A] = techComplexity(model)

A = (model.M ~= 0) - (diag(diag(model.M)) ~= 0);
alpha = ones(length(model.M), 1);
beta = ones(size(model.M));
gamma = 1;

value = sum(alpha) + sum(sum(beta.*A))*gamma*sum(svd(A));