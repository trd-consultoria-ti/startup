package controller

// ErroResponse é o envelope JSON de erro retornado pela API.
type ErroResponse struct {
	Sucesso  bool        `json:"sucesso" example:"false" description:"Representa se a operação foi bem-sucedida"`
	Mensagem string      `json:"mensagem" example:"Mensagem informativa" description:"Mensagem informativa sobre o resultado da operação"`
	Conteudo interface{} `json:"content,omitempty" description:"Objeto com os dados adicionais"`
}
