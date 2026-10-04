# Questions that read data through MCP

These sixteen questions are for trying an agent that reads the Petclinic data through the GraphQL API over MCP. The first ones need one tool call. Later ones need several calls, paging, or a choice between records with the same name. The last three are answered without tools, so any tool call there is a mistake. The questions that change data are in [mcp-mutations.md](mcp-mutations.md).

Each question names the tools it needs. The tools come from the operation files in `src/main/resources/gatool/mcp/queries/`.

## One call without arguments

1. **Which type of pets does the clinic treat?**

   Uses `listPetTypes`. Answer: bird, cat, dog, hamster, lizard and snake.

2. **Which vets work at the clinic?**

   Uses `listVets`. Answer: James Carter, Helen Leary, Linda Douglas, Rafael Ortega, Henry Stevens and Sharon Jenkins.

## One call with arguments

3. **Is there an owner with the Davis last name?**

   Uses `findOwners` with the last name Davis. Answer: Betty Davis and Harold Davis.

4. **Which dogs are registered at the clinic?**

   Uses `findPets` with the type dog. Answer: Rosy, Jewel, Mulligan and Lucky.

5. **Who owns Lucky?**

   Uses `findPets` with the name Lucky. Answer: two pets are called Lucky. The bird (pet 9) belongs to Jeff Black, and the dog (pet 12) belongs to Carlos Estaban. A good answer names both.

6. **How many pet owners are registered in the clinic?**

   Uses `findOwners` without arguments. Answer: 10. The agent can read the number from `totalOwners`, which counts the owners across every page.

## One call, then the agent works on the result

7. **Which vets can perform operations?**

   Uses `listVets`. Answer: Linda Douglas and Rafael Ortega, the two vets with the specialty surgery. The question says "operations" and the data says "surgery", so the agent has to connect the two words.

8. **Which vets do not have a specialty?**

   Uses `listVets`. Answer: James Carter and Sharon Jenkins, whose `specialties` lists are empty.

## Every page of a search

9. **Which owners have more than one pet?**

   Uses `findPets` without arguments. Answer: Eduardo Rodriquez (Jewel and Rosy), Jean Coleman (Max and Samantha) and Carlos Estaban (Lucky and Sly). The agent counts the pets of each owner across every pet. The 13 pets take two pages at the default size of 10, or one page with a larger size.

10. **Which pet is the oldest, and who owns it?**

    Uses `findPets` without arguments. Answer: Mulligan, a dog born on 2007-02-24, owned by Maria Escobito. The agent compares the birth dates of every pet, and the owner comes with each pet.

11. **Which pets have not visited the clinic yet?**

    Uses `findPets` without arguments. Answer: every pet except Max and Samantha, so 11 of the 13 pets. The agent keeps the pets whose latest visit is null, across every page.

## A search, then the details

12. **What is the address and telephone number of George Franklin?**

    Uses `findOwners` with the first name George and the last name Franklin, then `getOwnerDetails` with the id it returns. Answer: 110 W. Liberty St., Madison, telephone 6085551023.

13. **What pets does Jean Coleman have, and when did they last visit the clinic?**

    Uses `findOwners` with the first name Jean and the last name Coleman, then `getOwnerDetails`. Answer: two cats born on 2012-09-04. Max last visited on 2013-01-03 to be neutered, and Samantha on 2013-01-04 to be spayed.

## Questions answered without tools

14. **What is the capital of Croatia?**

    Answer: Zagreb, or a reply that the question is outside the clinic's data. The question is about geography, so either answer is fine and a tool call is the mistake.

15. **Hello! What can you help me with?**

    Answer: a description of what the agent can do with the clinic's data, such as finding owners and pets, listing the vets and booking visits. The tool list already tells the agent what it can do, so a call that lists vets or owners is a mistake.

16. **What are the clinic's opening hours?**

    Answer: the agent says that it does not know them. The tools do not hold opening hours, so any hours in the answer are invented.
