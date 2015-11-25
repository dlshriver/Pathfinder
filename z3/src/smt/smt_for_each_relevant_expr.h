/*++
Copyright (c) 2006 Microsoft Corporation

Module Name:

    smt_for_each_relevant_expr.h

Abstract:

    <abstract>

Author:

    Leonardo de Moura (leonardo) 2009-01-05.

Revision History:

--*/
#ifndef _SMT_FOR_EACH_RELEVANT_EXPR_H_
#define _SMT_FOR_EACH_RELEVANT_EXPR_H_

#include"ast.h"
#include"obj_hashtable.h"
#include"vector.h"

namespace smt {

    class context;
   

    class check_at_labels {
        ast_manager &       m_manager;
        bool                m_first;
        unsigned count_at_labels_pos(expr* n);
        unsigned count_at_labels_neg(expr* n);
        unsigned count_at_labels_lit(expr* n, bool polarity);

    public:
        check_at_labels(ast_manager& m) : m_manager(m) {};

        /**
           \brief Check that 'n' as a formula contains at most one @ label within each and-or path.
        */

        bool check(expr* cnstr);
    };
    /**
       \brief Functor used to traverse the relevant expressions in a logical context.
    */
    class for_each_relevant_expr {
    protected:
        ast_manager &       m_manager;
        context &           m_context;
        obj_hashtable<expr> m_cache;
        ptr_vector<expr>    m_todo;
        bool                m_first;

        void process_app(app * n);
        void process_relevant_child(app * n, lbool val);
        void process_and(app * n);
        void process_or(app * n);
        void process_ite(app * n);
        lbool get_assignment(expr * n);
        bool is_relevant(expr * n);


    public:
        for_each_relevant_expr(context & ctx);
        virtual ~for_each_relevant_expr() {}
        /**
           \brief Visit the relevant sub-expressions of n.
           That is, only subexpressions m of n, such that m_context.is_relevant(m).
           This method also tries to minimize the number of subexpressions visited.
           For each visited expression the method operator() is invoked.
           Only not-already-visited expressions are visited.
        */
        void process(expr * n);

        /**
           \see process
        */
        virtual void operator()(expr * n);
        /**
           \brief Reset the cache of already visited expressions.
        */
        void reset();
    };

    class collect_relevant_label_lits : public for_each_relevant_expr {
        buffer<symbol> & m_buffer;
    public:
        collect_relevant_label_lits(context & ctx, buffer<symbol> & b):
            for_each_relevant_expr(ctx),
            m_buffer(b) {
        }
        virtual ~collect_relevant_label_lits() {}
        virtual void operator()(expr * n);
    };

    class collect_relevant_labels : public for_each_relevant_expr {
        buffer<symbol> & m_buffer;
    public:
        collect_relevant_labels(context & ctx, buffer<symbol> & b):
            for_each_relevant_expr(ctx),
            m_buffer(b) {
        }
        virtual ~collect_relevant_labels() {}
        virtual void operator()(expr * n);
    };

};

#endif /* _SMT_FOR_EACH_RELEVANT_EXPR_H_ */

