/*% if (checkAnyEntityContainsPatternOfType(data, 'ipPattern') || checkAnyEntityContainsPatternOfType(data, 'urlPattern') || checkAnyEntityContainsPatternOfType(data, 'emailPattern')) { %*/
export default {
    /*% if (checkAnyEntityContainsPatternOfType(data, 'ipPattern')) { %*/
    // Regular expression used to validate IPs
    IP_REGEX: /^(([1-9]?[0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5]).){3}([1-9]?[0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$/,
    /*% } %*/
    /*% if (checkAnyEntityContainsPatternOfType(data, 'urlPattern')) { %*/

    // Regular expression used to validate URLs
    URL_REGEX: /^((https?|ftps?):\/\/)?(www.)?[-a-zA-Z0-9@:%._+~#=]{2,256}.[a-z]{2,6}\b([-a-zA-Z0-9@:%_+.~#?&//=]*)$/,
    /*% } %*/
    /*% if (checkAnyEntityContainsPatternOfType(data, 'emailPattern')) { %*/

    // Regular expression used to validate emails
    EMAIL_REGEX: /^[_a-z0-9-]+(.[_a-z0-9-]+)*@[a-z0-9-]+(.[a-z0-9-]+)*(.[a-z]{2,3})$/
    /*% } %*/
}
/*% } %*/