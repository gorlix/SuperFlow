module.exports = {
  root: true,
  env: {jest: true},
  extends: ['@react-native', 'plugin:jsdoc/recommended'],
  plugins: ['jsdoc', 'react'],
  rules: {
    'jsdoc/require-jsdoc': [
      'error',
      {
        require: {
          FunctionDeclaration: true,
          MethodDefinition: true,
          ClassDeclaration: true,
          ArrowFunctionExpression: true,
          FunctionExpression: true,
        },
      },
    ],
    'react/jsx-no-literals': [
      'error',
      {
        noStrings: true,
        allowedStrings: [],
        ignoreProps: true,
      },
    ],
  },
};
