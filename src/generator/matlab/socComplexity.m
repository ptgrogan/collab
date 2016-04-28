function [value, alpha, beta, gamma, A] = socComplexity(model)

A = (model.D ~= 0) - (diag(diag(model.D)) ~= 0);
alpha = zeros(length(model.D), 1);
beta = ones(size(model.D)) - eye(size(model.D));
gamma = 1;

value = sum(alpha) + sum(sum(beta.*A))*gamma*sum(svd(A));