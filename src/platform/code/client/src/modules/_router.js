/*%@ return data.dataModel.entities
      .filter(function(context){
         return !context.abstract;
      })
      .map(function(en) {
        return {
            fileName: normalize(en.name) + '/' + 'router.js',
            context: en
        };
      });
%*/
/*%
  var entity = getEntity(data, context.name);
  var theForms = data.forms.filter(function(form) { return form.entity == entity.name; });
  var theLists = data.lists.filter(function(list) { return list.entity == entity.name; });
%*/
/*% theLists.forEach(function (theList) { %*/
import /*%= normalize(theList.id) %*/List from "./components//*%= normalize(theList.id) %*/List";
/*% }); %*/

/*% theForms.forEach(function (theForm) { %*/
/*% if (theForm && (theForm.creatable || theForm.editable)) { %*/
import /*%= normalize(theForm.id) %*/Form from "./components//*%= normalize(theForm.id) %*/Form";
/*% } %*/
import /*%= normalize(theForm.id) %*/Detail from "./components//*%= normalize(theForm.id) %*/Detail";
/*% }); %*/

const routes = [
  /*% theLists.forEach(function (theList) { %*/
  {
    path: "//*%= normalizeUrl(theList.id) %*/",
    name: "/*%= theList.id %*/",
    component: /*%= normalize(theList.id) %*/List,
    meta: {
      label: "t_/*%= normalize(context.name) %*/.headers./*%= normalize(theList.id) %*/"
    }
  },
  /*% }); %*/
  /*% theForms.forEach(function (theForm) { %*/
    /*% if (theForm && theForm.editable) { %*/
  {
    path: "//*%= normalizeUrl(theForm.id) %*//:id/edit",
    name: "/*%= theForm.id %*/Form",
    component: /*%= normalize(theForm.id) %*/Form,
    meta: {
      label: "t_/*%= normalize(context.name) %*/.headers./*%= normalize(theForm.id) %*/"
    }
  },
  /*% } %*/
  /*% if (theForm && theForm.creatable) { %*/
  {
    path: "//*%= normalizeUrl(theForm.id) %*//new",
    name: "/*%= theForm.id %*/Create",
    component: /*%= normalize(theForm.id) %*/Form,
    meta: {
    label: "t_/*%= normalize(context.name) %*/.headers./*%= normalize(theForm.id) %*/"
    }
  },
  /*% } %*/
  /*% if (theForm) { %*/
  {
    path: "//*%= normalizeUrl(theForm.id) %*//:id(\\d+)",
    name: "/*%= theForm.id %*/Detail",
    component: /*%= normalize(theForm.id) %*/Detail,
    meta: {
      label: "t_/*%= normalize(context.name) %*/.headers./*%= normalize(theForm.id) %*/Detail"
    }
  },
    /*% } %*/
  /*% }); %*/
];

export default routes;
