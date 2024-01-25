/*%@ return data.dataModel.enums.map(function(en) {
  return {
    fileName: normalize(en.name.toLowerCase()) + '.js',
    context: en
  };
})
%*/
export default [
/*%
  context.values.forEach(function(val) {
%*/
  { text: "/*%= normalize(context.name.toLowerCase()) + '.' + val %*/", value: "/*%= val %*/"},
/*%
  });
%*/
];
